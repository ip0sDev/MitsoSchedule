package mitsoschedule.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * §5 Типографическая шкала Biolume.
 *
 * Гарнитура назначена ВСЕМ пятнадцати ролям, а не только перечисленным в таблице §5.
 * Непокрытая роль молча уходит на системный шрифт, и на одном экране оказываются две
 * гарнитуры — про это §5 предупреждает отдельно.
 *
 * tnum (tabular numerals) стоит везде: цифры получают одинаковую ширину, поэтому
 * тикающий отсчёт «до 14:30 (35 мин)» не дёргает соседний текст на каждом обновлении.
 *
 * Вес 700 не используется: §5 везёт Inter в 400/500/600, семисотый пришлось бы
 * синтезировать искусственным утолщением.
 */

private const val TabularNums = "tnum"

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = BiolumeSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = TabularNums,
    ),
    displayMedium = TextStyle(
        fontFamily = BiolumeSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
        fontFeatureSettings = TabularNums,
    ),
    displaySmall = TextStyle(
        fontFamily = BiolumeSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
        fontFeatureSettings = TabularNums,
    ),
    headlineLarge = TextStyle(
        fontFamily = BiolumeSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp,
        fontFeatureSettings = TabularNums,
    ),
    headlineMedium = TextStyle(
        fontFamily = BiolumeSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = TabularNums,
    ),
    headlineSmall = TextStyle(
        fontFamily = BiolumeSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = TabularNums,
    ),
    titleLarge = TextStyle(
        fontFamily = BiolumeSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = TabularNums,
    ),
    titleMedium = TextStyle(
        fontFamily = BiolumeSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
        fontFeatureSettings = TabularNums,
    ),
    titleSmall = TextStyle(
        fontFamily = BiolumeSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        fontFeatureSettings = TabularNums,
    ),
    bodyLarge = TextStyle(
        fontFamily = BiolumeSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
        fontFeatureSettings = TabularNums,
    ),
    bodyMedium = TextStyle(
        fontFamily = BiolumeSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
        fontFeatureSettings = TabularNums,
    ),
    bodySmall = TextStyle(
        fontFamily = BiolumeSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
        fontFeatureSettings = TabularNums,
    ),
    labelLarge = TextStyle(
        fontFamily = BiolumeSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        fontFeatureSettings = TabularNums,
    ),
    labelMedium = TextStyle(
        fontFamily = BiolumeSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        fontFeatureSettings = TabularNums,
    ),
    labelSmall = TextStyle(
        fontFamily = BiolumeSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        fontFeatureSettings = TabularNums,
    ),
)

/**
 * §1 принцип 5 «Данные — моноширинным».
 *
 * У M3 [Typography] нет слота под данные, поэтому роли раздаются отдельным
 * CompositionLocal'ом. Сюда идёт всё, что читается как значение, а не как текст:
 * диапазоны времени пар, отсчёты, суммы баланса, номера аудиторий, версии, метки
 * последнего обновления.
 */
@Immutable
data class BiolumeDataTypography(
    /** Крупное значение — суммы баланса и задолженности. */
    val dataLarge: TextStyle,
    /** Основное — время пары «09.00-10.20», отсчёты. */
    val dataMedium: TextStyle,
    /** Подписи — версии, аудитории, время последнего обновления. */
    val dataSmall: TextStyle,
)

val BiolumeDataTypographyDefaults = BiolumeDataTypography(
    dataLarge = TextStyle(
        fontFamily = BiolumeMono,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = TabularNums,
    ),
    dataMedium = TextStyle(
        fontFamily = BiolumeMono,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = TabularNums,
    ),
    dataSmall = TextStyle(
        fontFamily = BiolumeMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = TabularNums,
    ),
)

val LocalBiolumeDataType = staticCompositionLocalOf { BiolumeDataTypographyDefaults }
