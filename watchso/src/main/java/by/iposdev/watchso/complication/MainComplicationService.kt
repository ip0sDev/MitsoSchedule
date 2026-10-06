package by.iposdev.watchso.complication

import mitsoschedule.core.schedule.TodayStatus
import by.iposdev.watchso.R
import mitsoschedule.core.schedule.ScheduleDates
import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import mitsoschedule.core.schedule.TodayScheduleState
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
                applicationContext.getString(R.string.group),
                applicationContext.getString(R.string.mitso),
                applicationContext.getString(R.string.complication_group_not_selected),
                applicationContext.getString(R.string.app_name)
            )
        } else {
            val todaySched = ScheduleDates.findTodaySchedule(schedules)
            if (todaySched == null || todaySched.lessons.isEmpty()) {
                Quadruple(
                    applicationContext.getString(R.string.complication_no_lessons),
                    applicationContext.getString(R.string.mitso),
                    applicationContext.getString(R.string.complication_no_lessons_text),
                    applicationContext.getString(R.string.complication_group_line, selection.groupName)
                )
            } else {
                val timeInfo = TodayStatus.calculate(todaySched.lessons)
                when (timeInfo.state) {
                    TodayScheduleState.ONGOING_LESSON -> {
                        val lesson = timeInfo.currentLesson
                        val roomOrTime = lesson?.room?.removePrefix("ауд.")?.removePrefix("каб.")?.trim()
                            ?: lesson?.time?.substringBefore("—")?.trim()
                            ?: applicationContext.getString(R.string.lesson)
                        val title = applicationContext.getString(R.string.complication_now)
                        val lText = lesson?.subject ?: applicationContext.getString(R.string.complication_now_lesson)
                        val lTitle = "${lesson?.time ?: ""} • ${lesson?.room ?: ""}".trim(' ', '•')
                        Quadruple(roomOrTime, title, lText, lTitle)
                    }
                    TodayScheduleState.BREAK_BETWEEN_LESSONS -> {
                        val next = timeInfo.nextLesson
                        val roomOrTime = next?.room?.removePrefix("ауд.")?.removePrefix("каб.")?.trim()
                            ?: applicationContext.getString(R.string.complication_next)
                        val title = if (timeInfo.minutesToNext > 0) "${timeInfo.minutesToNext}м" else applicationContext.getString(R.string.complication_next)
                        val lText = applicationContext.getString(R.string.complication_next_lesson, next?.subject ?: applicationContext.getString(R.string.lesson))
                        val nextStart = next?.time?.substringBefore("—")?.trim() ?: ""
                        val lTitle = applicationContext.getString(R.string.complication_break_at, nextStart, next?.room ?: "").trim()
                        Quadruple(roomOrTime, title, lText, lTitle)
                    }
                    TodayScheduleState.NOT_STARTED -> {
                        val first = timeInfo.nextLesson
                        val startTime = first?.time?.substringBefore("—")?.trim() ?: applicationContext.getString(R.string.lesson)
                        val title = applicationContext.getString(R.string.complication_first)
                        val lText = first?.subject ?: applicationContext.getString(R.string.lessons)
                        val lTitle = applicationContext.getString(R.string.complication_first_at, startTime, first?.room ?: "").trim(' ', '•')
                        Quadruple(startTime, title, lText, lTitle)
                    }
                    TodayScheduleState.FINISHED -> {
                        Quadruple(
                            applicationContext.getString(R.string.complication_done),
                            applicationContext.getString(R.string.mitso),
                            applicationContext.getString(R.string.complication_done_text),
                            applicationContext.getString(R.string.complication_rest)
                        )
                    }
                    TodayScheduleState.NO_LESSONS -> {
                        Quadruple(
                            applicationContext.getString(R.string.complication_no_lessons),
                            applicationContext.getString(R.string.mitso),
                            applicationContext.getString(R.string.complication_no_lessons_today),
                            applicationContext.getString(R.string.complication_group_line, selection.groupName)
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