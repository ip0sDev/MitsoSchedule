package mitsoschedule.app.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Biolume v2.1 Color Palette
// Значения — строго из §3 Biolume-Design-Guidelines.md.
// Альфа в ARGB получена как round(alpha * 255):
//   .06 -> 0x0F   .10 -> 0x1A   .12 -> 0x1F   .14 -> 0x24
//   .16 -> 0x29   .18 -> 0x2E   .25 -> 0x40   .28 -> 0x47
// ==========================================

// 1. Abyss (Dark Theme)
val AbyssBackground = Color(0xFF0E1214)
val AbyssSurface = Color(0xFF141A1D)
val AbyssSurfaceContainerLow = Color(0xFF171E21)
val AbyssSurfaceContainer = Color(0xFF1A2126)
val AbyssSurfaceContainerHigh = Color(0xFF212930)
val AbyssSurfaceContainerHighest = Color(0xFF28313A)
val AbyssOnSurface = Color(0xFFECEFEE)
val AbyssOnSurfaceVariant = Color(0xFF93A0A0)
val AbyssOutline = Color(0xFF3F4948)
val AbyssOutlineVariant = Color(0x0FFFFFFF) // §3.1 rgba(255,255,255,.06)

val AbyssPrimary = Color(0xFF35C7E8) // Bioluminescent cyan
val AbyssOnPrimary = Color(0xFF00212B)
val AbyssPrimaryContainer = Color(0x1F35C7E8) // §3.1 .12
val AbyssOnPrimaryContainer = Color(0xFF35C7E8)

// §3.1 secondary — фиолетовый (медузы/сифонофоры), а НЕ копия primary
val AbyssSecondary = Color(0xFF8C6BFF)
val AbyssOnSecondary = Color(0xFF1B1033)
val AbyssSecondaryContainer = Color(0x248C6BFF) // §3.1 .14
val AbyssOnSecondaryContainer = Color(0xFFC9BBFF)

val AbyssTertiary = Color(0xFF6FC6FF) // Lunar blue
val AbyssOnTertiary = Color(0xFF012538)
val AbyssTertiaryContainer = Color(0x1F6FC6FF)
val AbyssOnTertiaryContainer = Color(0xFF6FC6FF)

val AbyssErrorContainer = Color(0x24FF4D6A)
val AbyssOnErrorContainer = Color(0xFFFF4D6A)
val AbyssOnError = Color(0xFF490013)

val AbyssSurfaceDim = Color(0xFF0E1214)
val AbyssSurfaceBright = Color(0xFF28313A)
val AbyssInverseSurface = Color(0xFFECEFEE)
val AbyssInverseOnSurface = Color(0xFF1C1F1E)
val AbyssInversePrimary = Color(0xFF0E7FA3)

// §4.2 selectionFill — плотная НЕпрозрачная смесь surfaceContainer + primary ≈20 %
// (#1A2126 ⊕ #35C7E8 @ 20 %). Не путать с primaryContainer (§10).
val AbyssSelectionFill = Color(0xFF1F424D)

// §3.1 glow — сигнальный слой, только focus/press/live
val AbyssGlowPrimary = Color(0x4735C7E8)
val AbyssGlowSecondary = Color(0x478C6BFF)
val AbyssGlowTertiary = Color(0x406FC6FF)

// 2. Tidepool (Light Theme)
val TidepoolBackground = Color(0xFFF2F0E9)
val TidepoolSurface = Color(0xFFF8F6F0)
val TidepoolSurfaceContainerLow = Color(0xFFF1EFE6)
val TidepoolSurfaceContainer = Color(0xFFECEAE0)
val TidepoolSurfaceContainerHigh = Color(0xFFE2DFD1)
val TidepoolSurfaceContainerHighest = Color(0xFFD8D4C3)
val TidepoolOnSurface = Color(0xFF1C1F1E)
val TidepoolOnSurfaceVariant = Color(0xFF5C625E)
val TidepoolOutline = Color(0xFF8E928E)
val TidepoolOutlineVariant = Color(0x0F000000) // §3.2 rgba(0,0,0,.06)

val TidepoolPrimary = Color(0xFF0E7FA3) // Deep cyan-teal
val TidepoolOnPrimary = Color(0xFFFFFFFF)
val TidepoolPrimaryContainer = Color(0x1A0E7FA3) // §3.2 .10
val TidepoolOnPrimaryContainer = Color(0xFF0E7FA3)

val TidepoolSecondary = Color(0xFF6A4FE0)
val TidepoolOnSecondary = Color(0xFFFFFFFF)
val TidepoolSecondaryContainer = Color(0x1A6A4FE0) // §3.2 .10
val TidepoolOnSecondaryContainer = Color(0xFF2A1A6B)

val TidepoolTertiary = Color(0xFF1B76B0)
val TidepoolOnTertiary = Color(0xFFFFFFFF)
val TidepoolTertiaryContainer = Color(0x1A1B76B0)
val TidepoolOnTertiaryContainer = Color(0xFF1B76B0)

val TidepoolErrorContainer = Color(0x1AD6304A)
val TidepoolOnErrorContainer = Color(0xFFD6304A)
val TidepoolOnError = Color(0xFFFFFFFF)

val TidepoolSurfaceDim = Color(0xFFD8D4C3)
val TidepoolSurfaceBright = Color(0xFFF8F6F0)
val TidepoolInverseSurface = Color(0xFF1C1F1E)
val TidepoolInverseOnSurface = Color(0xFFECEFEE)
val TidepoolInversePrimary = Color(0xFF35C7E8)

// §4.2 selectionFill — #ECEAE0 ⊕ #0E7FA3 @ 14 %. Холодный камень, без зелёного отлива.
val TidepoolSelectionFill = Color(0xFFCDDBD7)

val TidepoolGlowPrimary = Color(0x2E0E7FA3)
val TidepoolGlowSecondary = Color(0x296A4FE0)
val TidepoolGlowTertiary = Color(0x291B76B0)

// Common Status Colors (§3 — у M3 нет ролей success/warning, раздаются через BiolumeStatusColors)
val BiolumeSuccessDark = Color(0xFFA8DB6E)
val BiolumeSuccessLight = Color(0xFF4C9A2A)
val BiolumeWarningDark = Color(0xFFFFC24E)
val BiolumeWarningLight = Color(0xFFB8790A)
val BiolumeErrorDark = Color(0xFFFF4D6A)
val BiolumeErrorLight = Color(0xFFD6304A)

// Затемнение под модальными поверхностями — одинаково в обеих темах.
val BiolumeScrim = Color(0xFF000000)

// Biolume Lesson Badges
val LectureBadgeBgDark = Color(0x2435C7E8)
val LectureBadgeTextDark = Color(0xFF35C7E8)
val LectureBadgeBgLight = Color(0x240E7FA3)
val LectureBadgeTextLight = Color(0xFF0E7FA3)

val PracticeBadgeBgDark = Color(0x22FFC24E)
val PracticeBadgeTextDark = Color(0xFFFFC24E)
val PracticeBadgeBgLight = Color(0x22B8790A)
val PracticeBadgeTextLight = Color(0xFFB8790A)

val LabBadgeBgDark = Color(0x246FC6FF)
val LabBadgeTextDark = Color(0xFF6FC6FF)
val LabBadgeBgLight = Color(0x241B76B0)
val LabBadgeTextLight = Color(0xFF1B76B0)

val ExamBadgeBgDark = Color(0x22FF4D6A)
val ExamBadgeTextDark = Color(0xFFFF4D6A)
val ExamBadgeBgLight = Color(0x22D6304A)
val ExamBadgeTextLight = Color(0xFFD6304A)
