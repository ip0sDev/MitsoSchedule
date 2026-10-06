package mitsoschedule.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.model.StudentAuthCredentials
import mitsoschedule.core.model.StudentCabinetData
import mitsoschedule.core.model.UserSelection
import mitsoschedule.core.testing.InMemoryDataStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.crypto.KeyGenerator

class ScheduleStoreTest {

    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val cipher = AesGcmSecretCipher { key }
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: ScheduleStore

    private val credsKey = stringPreferencesKey("student_auth_credentials")
    private val cabinetKey = stringPreferencesKey("student_cached_cabinet_data")

    @Before
    fun setUp() {
        dataStore = InMemoryDataStore()
        store = ScheduleStore(dataStore, keyPrefix = "", cipher = cipher)
    }

    private suspend fun raw(name: Preferences.Key<String>) = dataStore.data.first()[name]

    @Test
    fun credentialsAreStoredEncrypted() = runBlocking {
        store.saveStudentCredentials(StudentAuthCredentials("419445", "bbb01937", true))

        val stored = raw(credsKey)!!
        assertTrue(stored.startsWith(SecretCipher.PREFIX))
        assertFalse(stored.contains("bbb01937"))
        assertEquals(StudentAuthCredentials("419445", "bbb01937", true), store.studentCredentialsFlow.first())
    }

    @Test
    fun cabinetCacheIsStoredEncrypted() = runBlocking {
        store.saveStudentCabinetData(StudentCabinetData(fullName = "Корзун Д. А.", moodlePassword = "bbb01937"))

        assertFalse(raw(cabinetKey)!!.contains("bbb01937"))
        assertEquals("bbb01937", store.cachedStudentCabinetFlow.first()?.moodlePassword)
    }

    @Test
    fun legacyPlaintextIsReadableAndMigrated() = runBlocking {
        dataStore.edit {
            it[credsKey] = """{"login":"419445","password":"bbb01937","rememberMe":true}"""
        }
        // до миграции читается как раньше
        assertEquals("bbb01937", store.studentCredentialsFlow.first()?.password)

        store.migrateLegacySecrets()

        val stored = raw(credsKey)!!
        assertTrue(stored.startsWith(SecretCipher.PREFIX))
        assertFalse(stored.contains("bbb01937"))
        assertEquals("bbb01937", store.studentCredentialsFlow.first()?.password)
    }

    @Test
    fun undecryptableSecretIsTreatedAsMissing() = runBlocking {
        store.saveStudentCredentials(StudentAuthCredentials("419445", "bbb01937"))
        val otherKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val rebuilt = ScheduleStore(dataStore, "", AesGcmSecretCipher { otherKey })

        assertNull(rebuilt.studentCredentialsFlow.first())
    }

    @Test
    fun failedEncryptionNeverWritesPlaintext() = runBlocking {
        val broken = object : SecretCipher {
            override fun encrypt(plain: String): String? = null
            override fun decrypt(token: String): String? = null
        }
        val brokenStore = ScheduleStore(dataStore, "", broken)

        brokenStore.saveStudentCredentials(StudentAuthCredentials("419445", "bbb01937"))
        assertNull(raw(credsKey))

        dataStore.edit { it[credsKey] = """{"login":"1","password":"p"}""" }
        brokenStore.migrateLegacySecrets()
        assertNull(raw(credsKey))
    }

    @Test
    fun selectionAndScheduleCacheRoundTrip() = runBlocking {
        store.saveSelection(UserSelection(facultyId = "f", courseId = "c", groupId = "g"))
        store.saveSchedule(
            listOf(DaySchedule(dayTitle = "Понедельник, 21 сентября", weekId = "4")),
            "21.09.2026 10:00",
            123L
        )

        assertTrue(store.getSavedSelection()!!.isComplete)
        assertEquals("Понедельник, 21 сентября", store.getCachedSchedule().single().dayTitle)
        assertEquals(123L, store.getLastFetchMillis())

        store.clearScheduleCache()
        assertTrue(store.getCachedSchedule().isEmpty())
        assertEquals(0L, store.getLastFetchMillis())
    }

    @Test
    fun keyPrefixSeparatesWatchKeys() = runBlocking {
        val watch = ScheduleStore(dataStore, "watch_", cipher)
        store.saveSelection(UserSelection(groupId = "phone"))
        watch.saveSelection(UserSelection(groupId = "watch"))

        assertEquals("phone", store.getSavedSelection()?.groupId)
        assertEquals("watch", watch.getSavedSelection()?.groupId)
    }
}
