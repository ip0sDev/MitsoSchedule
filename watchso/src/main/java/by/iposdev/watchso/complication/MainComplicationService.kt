package by.iposdev.watchso.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import by.iposdev.watchso.data.ScheduleTimeUtils
import by.iposdev.watchso.data.TodayScheduleState
import by.iposdev.watchso.data.WatchPreferencesManager
import by.iposdev.watchso.presentation.MainActivity

class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("312").build(),
                    contentDescription = PlainComplicationText.Builder("Аудитория 312").build()
                )
                    .setTitle(PlainComplicationText.Builder("Идёт").build())
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("Гражданское право").build(),
                    contentDescription = PlainComplicationText.Builder("Гражданское право в 312 аудитории").build()
                )
                    .setTitle(PlainComplicationText.Builder("08:15 • ауд. 312").build())
                    .build()
            }
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val prefs = WatchPreferencesManager(applicationContext)
        val selection = prefs.getSavedSelection()
        val schedules = prefs.getCachedSchedule()

        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapAction = PendingIntent.getActivity(
            applicationContext,
            0,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val (shortText, shortTitle, longText, longTitle) = if (selection == null || !selection.isComplete) {
            Quadruple(
                "Группа",
                "МИТСО",
                "Группа не выбрана",
                "МИТСО Расписание"
            )
        } else {
            val todaySched = ScheduleTimeUtils.findTodaySchedule(schedules)
            if (todaySched == null || todaySched.lessons.isEmpty()) {
                Quadruple(
                    "Пар нет",
                    "МИТСО",
                    "На сегодня занятий нет",
                    "МИТСО • ${selection.groupName}"
                )
            } else {
                val timeInfo = ScheduleTimeUtils.calculateTodayTimeInfo(todaySched.lessons)
                when (timeInfo.state) {
                    TodayScheduleState.ONGOING_LESSON -> {
                        val lesson = timeInfo.currentLesson
                        val roomOrTime = lesson?.room?.removePrefix("ауд.")?.removePrefix("каб.")?.trim()
                            ?: lesson?.time?.substringBefore("—")?.trim()
                            ?: "Пара"
                        val title = "Идёт"
                        val lText = lesson?.subject ?: "Идёт занятие"
                        val lTitle = "${lesson?.time ?: ""} • ${lesson?.room ?: ""}".trim(' ', '•')
                        Quadruple(roomOrTime, title, lText, lTitle)
                    }
                    TodayScheduleState.BREAK_BETWEEN_LESSONS -> {
                        val next = timeInfo.nextLesson
                        val roomOrTime = next?.room?.removePrefix("ауд.")?.removePrefix("каб.")?.trim()
                            ?: "След."
                        val title = if (timeInfo.minutesToNext > 0) "${timeInfo.minutesToNext}м" else "След."
                        val lText = "След: ${next?.subject ?: "Пара"}"
                        val nextStart = next?.time?.substringBefore("—")?.trim() ?: ""
                        val lTitle = "Перерыв • в $nextStart (${next?.room ?: ""})".trim()
                        Quadruple(roomOrTime, title, lText, lTitle)
                    }
                    TodayScheduleState.NOT_STARTED -> {
                        val first = timeInfo.nextLesson
                        val startTime = first?.time?.substringBefore("—")?.trim() ?: "Пара"
                        val title = "1-я"
                        val lText = first?.subject ?: "Занятия"
                        val lTitle = "1-я в $startTime • ${first?.room ?: ""}".trim(' ', '•')
                        Quadruple(startTime, title, lText, lTitle)
                    }
                    TodayScheduleState.FINISHED -> {
                        Quadruple(
                            "Всё!",
                            "МИТСО",
                            "Пары на сегодня завершены",
                            "МИТСО • Отдых"
                        )
                    }
                    TodayScheduleState.NO_LESSONS -> {
                        Quadruple(
                            "Пар нет",
                            "МИТСО",
                            "Сегодня нет пар",
                            "МИТСО • ${selection.groupName}"
                        )
                    }
                }
            }
        }

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(shortText).build(),
                    contentDescription = PlainComplicationText.Builder("$shortTitle: $shortText").build()
                )
                    .setTitle(PlainComplicationText.Builder(shortTitle).build())
                    .setTapAction(tapAction)
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(longText).build(),
                    contentDescription = PlainComplicationText.Builder("$longTitle: $longText").build()
                )
                    .setTitle(PlainComplicationText.Builder(longTitle).build())
                    .setTapAction(tapAction)
                    .build()
            }
            else -> null
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}