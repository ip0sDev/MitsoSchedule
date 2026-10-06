package mitsoschedule.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StudentWebWorkerTest {

    @Test
    fun testStudentCabinetParser() {
        val studentWorker = StudentWebWorker()
        val sampleHtml = """
            <html>
            <body>
                <div class="header">СТУДЕНТ</div>
                <div class="student-name">Корзун Денис Алексеевич</div>
                <div class="section-title">Состояние лицевого счета на конец дня 28-08-2026:</div>
                <div class="field">Баланс: 0.00</div>
                <div class="field">Основной долг: 0.00</div>
                <div class="field">Пеня за просрочку платежа + процент за пользование чужими денежными средствами: 0.00</div>
                <div class="notice">Важно! Данные обновляются ежедневно в 13.00, и соответствуют факту баланса лицевого счета за предыдущий день.</div>
                
                <div class="section-title">Доступ к системе дистанционного обучения на базе LMS Moodle</div>
                <div class="field">Группа: 2423</div>
                <div class="field">Логин: 419445</div>
                <div class="field">Пароль: bbb01937</div>
                <div class="notice">Уважаемые студенты! При первом входе в систему дистанционного обучения необходимо ввести и подтвердить адрес электронной почты!</div>
            </body>
            </html>
        """.trimIndent()

        val parsed = studentWorker.parseCabinetHtml(sampleHtml)
        assertEquals("Корзун Денис Алексеевич", parsed.fullName)
        assertEquals("Корзун Д. А.", parsed.initialsName)
        assertEquals("28-08-2026", parsed.accountDate)
        assertEquals("0.00", parsed.balance)
        assertEquals("0.00", parsed.mainDebt)
        assertEquals("0.00", parsed.penalty)
        assertEquals("2423", parsed.moodleGroup)
        assertEquals("419445", parsed.moodleLogin)
        assertEquals("bbb01937", parsed.moodlePassword)
        assertFalse(parsed.isDebt)
    }
}
