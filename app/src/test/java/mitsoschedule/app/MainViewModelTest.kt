package mitsoschedule.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import mitsoschedule.app.data.AppSettings
import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.model.OptionItem
import mitsoschedule.core.model.UserSelection
import mitsoschedule.core.network.StudentSession
import mitsoschedule.core.network.StudentWebWorker
import mitsoschedule.core.network.WebWorker
import mitsoschedule.core.schedule.ScheduleRepository
import mitsoschedule.core.storage.AesGcmSecretCipher
import mitsoschedule.core.storage.ScheduleStore
import mitsoschedule.core.testing.FakeServer
import mitsoschedule.core.testing.InMemoryDataStore
import mitsoschedule.core.ui.UiStrings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.crypto.KeyGenerator

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    /** Строки без Context: «<id ресурса>:<аргументы>», достаточно, чтобы отличать сообщения. */
    private object FakeStrings : UiStrings {
        override fun get(id: Int, vararg args: Any) = "$id:${args.joinToString(",")}"
    }

    private class FakeSettings : AppSettings {
        override val serverUrlFlow = MutableStateFlow("http://fake")
        override val themeModeFlow = MutableStateFlow("system")
        override suspend fun saveServerUrl(url: String) {
            serverUrlFlow.value = url
        }

        override suspend fun setThemeMode(mode: String) {
            themeModeFlow.value = mode
        }

        override val dynamicColorFlow = MutableStateFlow(false)

        override suspend fun setDynamicColor(enabled: Boolean) {
            dynamicColorFlow.value = enabled
        }
    }

    private val facultiesJson = """[{"id":"fac","name":"Юридический"}]"""
    private val weeksJson = """[{"id":"1","name":"w1"},{"id":"2","name":"w2"}]"""
    private val dayJson = """[{"dayTitle":"Понедельник, 31 августа","lessons":[{"time":"08.15 — 09.35","subject":"Предмет"}]}]"""
    private val cabinetJson = """{"fullName":"Корзун Денис Алексеевич","balance":"0.00"}"""

    private val selection = UserSelection(
        facultyId = "fac", courseId = "1", groupId = "g1", weekId = "1", weekName = "w1"
    )

    private lateinit var server: FakeServer
    private lateinit var store: ScheduleStore

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        server = FakeServer(
            mapOf(
                "/health" to """{"status":"healthy"}""",
                "/api/v1/schedule/faculties" to facultiesJson,
                "/api/v1/schedule/weeks" to weeksJson,
                "/api/v1/schedule" to dayJson,
                "/api/v1/student/cabinet" to cabinetJson
            )
        )
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        store = ScheduleStore(InMemoryDataStore(), "", AesGcmSecretCipher { key })
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(): MainViewModel {
        val client = server.client()
        return MainViewModel(
            scheduleRepository = ScheduleRepository(WebWorker("http://fake", client), store),
            studentSession = StudentSession(StudentWebWorker("http://fake", client), store),
            store = store,
            settings = FakeSettings(),
            strings = FakeStrings
        )
    }

    /** Запросы идут через реальный IO-диспетчер, поэтому ждём результата, а не фиксированное время. */
    private fun awaitUntil(message: String, condition: () -> Boolean) = runBlocking {
        try {
            withTimeout(5_000) { while (!condition()) delay(10) }
        } catch (e: Exception) {
            throw AssertionError("Не дождались: $message", e)
        }
    }

    @Test
    fun facultiesAreLoadedOnFirstStart() {
        val vm = newViewModel()
        awaitUntil("факультеты") { vm.faculties.value.isNotEmpty() }

        assertEquals(listOf(OptionItem("fac", "Юридический")), vm.faculties.value)
        assertNull(vm.errorMessage.value)
        awaitUntil("проверка сервера") { vm.serverHealth.value != null }
        assertTrue(vm.serverHealth.value!!.isHealthy)
    }

    @Test
    fun networkFailureIsShownWithReasonWhenFacultiesCannotBeLoaded() {
        server.goOffline()
        val vm = newViewModel()
        awaitUntil("ошибка") { vm.errorMessage.value != null }

        val expected = FakeStrings.get(
            R.string.error_load_faculties,
            FakeStrings.get(mitsoschedule.core.R.string.error_reason_network)
        )
        assertEquals(expected, vm.errorMessage.value)
        assertTrue(vm.faculties.value.isEmpty())
    }

    @Test
    fun applySelectionLoadsFiltersByWeekAndCaches() {
        val vm = newViewModel()
        vm.applySelection(selection)
        awaitUntil("расписание") { vm.scheduleData.value.isNotEmpty() }

        assertTrue(vm.scheduleData.value.all { it.weekId == vm.userSelection.value.weekId })
        assertEquals(listOf("1", "2"), vm.weeks.value.filter { it.id != MainViewModel.ALL_WEEKS_ID }.map { it.id })
        assertNotNull(vm.lastUpdateTime.value)
        awaitUntil("кэш") { runBlocking { store.getCachedSchedule().isNotEmpty() } }
    }

    @Test
    fun weekArrowsMoveBetweenRealWeeks() {
        val vm = newViewModel()
        vm.applySelection(selection)
        awaitUntil("расписание") { vm.scheduleData.value.isNotEmpty() }
        vm.onWeekSelected(OptionItem("1", "w1"))

        assertFalse(vm.canGoPreviousWeek)
        assertTrue(vm.canGoNextWeek)
        vm.selectNextWeek()
        assertEquals("2", vm.userSelection.value.weekId)
        assertTrue(vm.canGoPreviousWeek)
        assertFalse(vm.canGoNextWeek)
        vm.selectPreviousWeek()
        assertEquals("1", vm.userSelection.value.weekId)
    }

    @Test
    fun cachedScheduleIsShownOfflineWithoutError() {
        runBlocking {
            store.saveSelection(selection)
            store.saveSchedule(
                listOf(DaySchedule(dayTitle = "Понедельник, 31 августа", weekId = "1", weekName = "w1")),
                "вчера",
                System.currentTimeMillis()
            )
        }
        server.goOffline()

        val vm = newViewModel()
        awaitUntil("кэш в UI") { vm.scheduleData.value.isNotEmpty() }
        vm.fetchSchedule(isManualRefresh = true)
        awaitUntil("конец загрузки") {
            !vm.isLoading.value &&
                server.requests.any { it.startsWith("/api/v1/schedule/weeks") } &&
                // фоновая загрузка факультетов тоже успела упасть: ошибка не должна просочиться в экран
                server.requests.any { it.startsWith("/api/v1/schedule/faculties") } &&
                !vm.isLoadingOptions.value
        }

        assertEquals(1, vm.scheduleData.value.size)
        assertNull(vm.errorMessage.value)
        assertEquals("вчера", vm.lastUpdateTime.value)
    }

    @Test
    fun emptyAndFailedScheduleGiveDifferentMessages() {
        // сервер доступен, но расписание не опубликовано
        server.respond("/api/v1/schedule", "[]")
        val published = newViewModel()
        published.applySelection(selection)
        awaitUntil("сообщение") { published.errorMessage.value != null }
        assertEquals(FakeStrings.get(R.string.error_schedule_empty), published.errorMessage.value)

        // сервер недоступен
        server.goOffline()
        val offline = newViewModel()
        offline.applySelection(selection)
        awaitUntil("сообщение") { offline.errorMessage.value != null }
        assertEquals(
            FakeStrings.get(
                R.string.error_schedule_load,
                FakeStrings.get(mitsoschedule.core.R.string.error_reason_network)
            ),
            offline.errorMessage.value
        )
    }

    @Test
    fun fetchWithoutGroupAsksToChooseOne() {
        val vm = newViewModel()
        vm.fetchSchedule()
        assertEquals(FakeStrings.get(R.string.error_select_group_first), vm.errorMessage.value)
    }

    @Test
    fun studentLoginIsRememberedAndLogoutClearsIt() {
        val vm = newViewModel()
        vm.loginStudent("419445", "secret", rememberMe = true)
        awaitUntil("кабинет") { vm.studentCabinetData.value != null }

        assertEquals("Корзун Денис Алексеевич", vm.studentCabinetData.value?.fullName)
        awaitUntil("сохранение") { runBlocking { store.studentCredentialsFlow.first() } != null }

        vm.logoutStudent()
        awaitUntil("выход") { runBlocking { store.studentCredentialsFlow.first() } == null }
        assertNull(vm.studentCabinetData.value)
    }

    @Test
    fun studentLoginWithoutRememberIsNotStored() {
        val vm = newViewModel()
        vm.loginStudent("419445", "secret", rememberMe = false)
        awaitUntil("кабинет") { vm.studentCabinetData.value != null }

        assertNull(runBlocking { store.studentCredentialsFlow.first() })
    }

    @Test
    fun studentLoginFailureShowsMessage() {
        server.goOffline()
        val vm = newViewModel()
        vm.loginStudent("419445", "secret", rememberMe = true)
        awaitUntil("ошибка") { vm.studentErrorMessage.value != null }

        assertNull(vm.studentCabinetData.value)
        assertFalse(vm.isStudentLoading.value)
    }

    @Test
    fun dynamicColorChoiceIsStoredAndRestored() {
        val settings = FakeSettings()
        settings.dynamicColorFlow.value = true
        val client = server.client()
        val vm = MainViewModel(
            scheduleRepository = ScheduleRepository(WebWorker("http://fake", client), store),
            studentSession = StudentSession(StudentWebWorker("http://fake", client), store),
            store = store,
            settings = settings,
            strings = FakeStrings
        )
        awaitUntil("восстановление настройки") { vm.useDynamicColor.value }

        vm.setDynamicColor(false)
        assertFalse(vm.useDynamicColor.value)
        awaitUntil("сохранение") { !settings.dynamicColorFlow.value }
    }

    @Test
    fun tabSwitching() {
        val vm = newViewModel()
        assertEquals(AppTab.SCHEDULE, vm.currentTab.value)
        vm.selectTab(AppTab.SETTINGS)
        assertEquals(AppTab.SETTINGS, vm.currentTab.value)
    }

    @Test
    fun serverUrlChangeIsPersistedAndReloadsData() {
        val vm = newViewModel()
        vm.updateServerUrl("http://other/")
        awaitUntil("сохранение адреса") { vm.serverUrl.value == "http://other" }
        assertEquals("http://other", vm.serverUrl.value)
    }
}
