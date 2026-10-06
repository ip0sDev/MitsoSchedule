package by.iposdev.watchso

import by.iposdev.watchso.presentation.WearScreen
import by.iposdev.watchso.presentation.viewmodel.MainViewModel
import by.iposdev.watchso.presentation.viewmodel.WearPickerStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.crypto.KeyGenerator

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private object FakeStrings : UiStrings {
        override fun get(id: Int, vararg args: Any) = "$id:${args.joinToString(",")}"
    }

    private val selection = UserSelection(
        facultyId = "fac", formId = "Dnevnaya", courseId = "kurs1", groupId = "g1", weekId = "1", weekName = "w1"
    )

    private lateinit var server: FakeServer
    private lateinit var store: ScheduleStore
    private var scheduleUpdates = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        server = FakeServer(
            mapOf(
                "/api/v1/schedule/faculties" to """[{"id":"fac","name":"Юридический"}]""",
                "/api/v1/schedule/forms" to """[{"id":"Dnevnaya","name":"Дневная"}]""",
                "/api/v1/schedule/courses" to """[{"id":"1","name":"1 курс"}]""",
                "/api/v1/schedule/groups" to """[{"id":"g1","name":"2631 ПР"}]""",
                "/api/v1/schedule/weeks" to """[{"id":"1","name":"w1"},{"id":"2","name":"w2"}]""",
                "/api/v1/schedule" to """[{"dayTitle":"Понедельник, 31 августа","lessons":[{"time":"08.15 — 09.35","subject":"Предмет"}]}]""",
                "/api/v1/student/cabinet" to """{"fullName":"Корзун Денис Алексеевич"}"""
            )
        )
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        store = ScheduleStore(InMemoryDataStore(), "watch_", AesGcmSecretCipher { key })
        scheduleUpdates = 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(): MainViewModel {
        val client = server.client()
        return MainViewModel(
            scheduleRepository = ScheduleRepository(WebWorker("http://fake", client), store, "dd.MM HH:mm"),
            studentSession = StudentSession(StudentWebWorker("http://fake", client), store),
            store = store,
            strings = FakeStrings,
            onScheduleUpdated = { scheduleUpdates++ }
        )
    }

    private fun awaitUntil(message: String, condition: () -> Boolean) = runBlocking {
        try {
            withTimeout(5_000) { while (!condition()) delay(10) }
        } catch (e: Exception) {
            throw AssertionError("Не дождались: $message", e)
        }
    }

    @Test
    fun withoutSavedGroupFacultiesAreLoaded() {
        val vm = newViewModel()
        awaitUntil("факультеты") { vm.faculties.value.isNotEmpty() }
        assertEquals(OptionItem("fac", "Юридический"), vm.faculties.value.single())
    }

    @Test
    fun pickingGroupReturnsToScheduleAndNotifiesTile() {
        val vm = newViewModel()
        vm.navigateTo(WearScreen.PICKER)
        awaitUntil("факультеты") { vm.faculties.value.isNotEmpty() }

        vm.onFacultySelected(vm.faculties.value.first())
        awaitUntil("курсы") { vm.courses.value.isNotEmpty() }
        assertEquals(WearPickerStep.COURSE, vm.pickerStep.value)

        vm.onCourseSelected(vm.courses.value.first())
        awaitUntil("группы") { vm.groups.value.isNotEmpty() }
        assertEquals(WearPickerStep.GROUP, vm.pickerStep.value)

        vm.onGroupSelected(vm.groups.value.first())
        assertEquals(WearScreen.SCHEDULE, vm.currentScreen.value)
        awaitUntil("расписание") { vm.scheduleData.value.isNotEmpty() }

        assertTrue(vm.userSelection.value.isComplete)
        assertTrue("плитка должна обновиться после загрузки", scheduleUpdates > 0)
        awaitUntil("кэш") { runBlocking { store.getCachedSchedule().isNotEmpty() } }
    }

    @Test
    fun savedGroupShowsCacheAndKeepsItWhenRefreshFails() {
        runBlocking {
            store.saveSelection(selection)
            store.saveSchedule(
                listOf(mitsoschedule.core.model.DaySchedule(dayTitle = "Понедельник, 31 августа", weekId = "1", weekName = "w1")),
                "вчера",
                System.currentTimeMillis()
            )
        }
        server.goOffline()

        val vm = newViewModel()
        awaitUntil("кэш в UI") { vm.scheduleData.value.isNotEmpty() }
        vm.fetchSchedule(isManualRefresh = true)
        awaitUntil("конец загрузки") { !vm.isLoading.value && server.requests.isNotEmpty() }

        assertEquals(1, vm.scheduleData.value.size)
        assertEquals(null, vm.errorMessage.value)
        assertEquals(0, scheduleUpdates)
    }

    @Test
    fun emptyAndOfflineScheduleGiveDifferentMessages() {
        server.respond("/api/v1/schedule", "[]")
        runBlocking { store.saveSelection(selection) }
        val published = newViewModel()
        awaitUntil("сообщение") { published.errorMessage.value != null }
        assertEquals(FakeStrings.get(R.string.error_schedule_empty), published.errorMessage.value)

        server.goOffline()
        runBlocking { store.clearScheduleCache() }
        val offline = newViewModel()
        awaitUntil("сообщение") { offline.errorMessage.value != null }
        assertEquals(
            FakeStrings.get(R.string.error_schedule_load, FakeStrings.get(mitsoschedule.core.R.string.error_reason_network)),
            offline.errorMessage.value
        )
    }

    @Test
    fun obsoleteNumericSelectionIsReset() {
        runBlocking { store.saveSelection(UserSelection(facultyId = "12", courseId = "3", groupId = "g1")) }
        val vm = newViewModel()
        awaitUntil("сброс") { runBlocking { store.getSavedSelection() }?.isComplete == false }

        assertFalse(vm.userSelection.value.isComplete)
    }

    @Test
    fun cabinetLoginIsStoredAndLogoutClearsIt() {
        val vm = newViewModel()
        vm.loginStudent("419445", "secret")
        awaitUntil("кабинет") { vm.studentCabinetData.value != null }
        awaitUntil("сохранение") { runBlocking { store.studentCredentialsFlow.first() } != null }

        vm.logoutStudent()
        awaitUntil("выход") { vm.studentCabinetData.value == null }
        awaitUntil("очистка") { runBlocking { store.studentCredentialsFlow.first() } == null }
    }
}
