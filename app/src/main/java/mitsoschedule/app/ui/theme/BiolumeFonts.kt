package mitsoschedule.app.ui.theme

import androidx.compose.ui.text.font.FontFamily

/*
 * §5 Типографика — единственная точка, где объявляются гарнитуры.
 *
 * Гайд просит пару Inter (интерфейс) + JetBrains Mono (данные). TTF под OFL кладутся
 * в app/src/main/res/font/, после чего здесь меняются ровно две val'ы:
 *
 *     val BiolumeSans = FontFamily(
 *         Font(R.font.inter_regular,  FontWeight.Normal),
 *         Font(R.font.inter_medium,   FontWeight.Medium),
 *         Font(R.font.inter_semibold, FontWeight.SemiBold),
 *     )
 *     val BiolumeMono = FontFamily(Font(R.font.jetbrains_mono_medium, FontWeight.Medium))
 *
 * До этого момента роли держатся на системных гарнитурах. Важно, что структура §5
 * работает уже сейчас: разделение «интерфейс / данные» и правило «данные — моноширинным»
 * (§1 п.5) дают эффект и на Roboto + Roboto Mono, а замена гарнитуры потом не трогает
 * ни один компонент.
 *
 * Space Grotesk сознательно не подключается: §5 разрешает его только для латиницы,
 * а вордмарк приложения — «МИТСО Расписание». Заголовочные роли идут на Sans.
 */

/** Интерфейсная гарнитура. → Inter 400/500/600. */
val BiolumeSans: FontFamily = FontFamily.Default

/** Гарнитура данных: время пар, отсчёты, суммы, версии. → JetBrains Mono 500. */
val BiolumeMono: FontFamily = FontFamily.Monospace
