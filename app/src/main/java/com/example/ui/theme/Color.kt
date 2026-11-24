package com.example.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.R

enum class ThemeMode(@StringRes val labelRes: Int) {
  SYSTEM(R.string.theme_system),
  LIGHT(R.string.theme_light),
  DARK(R.string.theme_dark),
}

enum class AppPalette(
  @StringRes val labelRes: Int,
  val previewPrimary: Color,
  val previewSecondary: Color,
) {
  OCEAN_TEAL(R.string.palette_ocean_teal, Color(0xFF0E7490), Color(0xFFF97316)),
  EMERALD_SAGE(R.string.palette_emerald_sage, Color(0xFF15803D), Color(0xFF0D9488)),
  ROYAL_INDIGO(R.string.palette_royal_indigo, Color(0xFF4338CA), Color(0xFFEC4899)),
  TERRACOTTA_CORAL(R.string.palette_terracotta_coral, Color(0xFFC2410C), Color(0xFF0284C7)),
  AMBER_GOLD(R.string.palette_amber_gold, Color(0xFFB45309), Color(0xFF0F766E)),
  BERRY_PLUM(R.string.palette_berry_plum, Color(0xFF86198F), Color(0xFF059669)),
}

val CheckedShoppingRedLight = Color(0xFFD32F2F)
val CheckedShoppingRedDark = Color(0xFFFF5252)

fun lightSchemeFor(palette: AppPalette): ColorScheme =
  when (palette) {
    AppPalette.OCEAN_TEAL ->
      lightColorScheme(
        primary = Color(0xFF0E7490),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFCFFAFE),
        onPrimaryContainer = Color(0xFF083344),
        secondary = Color(0xFFEA580C),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFEDD5),
        onSecondaryContainer = Color(0xFF431407),
        tertiary = Color(0xFF0F766E),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFCCFBF1),
        onTertiaryContainer = Color(0xFF134E4A),
        background = Color(0xFFF7FAFC),
        onBackground = Color(0xFF0F172A),
        surface = Color(0xFFF7FAFC),
        onSurface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFFE2E8F0),
        onSurfaceVariant = Color(0xFF475569),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF1F5F9),
        surfaceContainer = Color(0xFFE8EFF5),
        surfaceContainerHigh = Color(0xFFDFE8F0),
        outline = Color(0xFF94A3B8),
        outlineVariant = Color(0xFFCBD5E1),
      )

    AppPalette.EMERALD_SAGE ->
      lightColorScheme(
        primary = Color(0xFF15803D),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDCFCE7),
        onPrimaryContainer = Color(0xFF052E16),
        secondary = Color(0xFF0D9488),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCCFBF1),
        onSecondaryContainer = Color(0xFF134E4A),
        tertiary = Color(0xFFD97706),
        onTertiary = Color.White,
        background = Color(0xFFF7FBF8),
        onBackground = Color(0xFF0F1F14),
        surface = Color(0xFFF7FBF8),
        onSurface = Color(0xFF0F1F14),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFEEF7F0),
        surfaceContainer = Color(0xFFE4F2E7),
        surfaceContainerHigh = Color(0xFFD8EBDC),
      )

    AppPalette.ROYAL_INDIGO ->
      lightColorScheme(
        primary = Color(0xFF4338CA),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE0E7FF),
        onPrimaryContainer = Color(0xFF1E1B4B),
        secondary = Color(0xFFDB2777),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFCE7F3),
        onSecondaryContainer = Color(0xFF500724),
        tertiary = Color(0xFF0284C7),
        onTertiary = Color.White,
        background = Color(0xFFF8F9FE),
        onBackground = Color(0xFF131326),
        surface = Color(0xFFF8F9FE),
        onSurface = Color(0xFF131326),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF1F2FC),
        surfaceContainer = Color(0xFFE7E9FA),
        surfaceContainerHigh = Color(0xFFDDE0F7),
      )

    AppPalette.TERRACOTTA_CORAL ->
      lightColorScheme(
        primary = Color(0xFFC2410C),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFEDD5),
        onPrimaryContainer = Color(0xFF431407),
        secondary = Color(0xFF0284C7),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0F2FE),
        onSecondaryContainer = Color(0xFF0C4A6E),
        tertiary = Color(0xFF65A30D),
        onTertiary = Color.White,
        background = Color(0xFFFFFBF8),
        onBackground = Color(0xFF231611),
        surface = Color(0xFFFFFBF8),
        onSurface = Color(0xFF231611),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFDF3EC),
        surfaceContainer = Color(0xFFF9EAE0),
        surfaceContainerHigh = Color(0xFFF4DFD1),
      )

    AppPalette.AMBER_GOLD ->
      lightColorScheme(
        primary = Color(0xFFB45309),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFEF3C7),
        onPrimaryContainer = Color(0xFF451A03),
        secondary = Color(0xFF0F766E),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCCFBF1),
        onSecondaryContainer = Color(0xFF134E4A),
        tertiary = Color(0xFF4F46E5),
        onTertiary = Color.White,
        background = Color(0xFFFFFCF5),
        onBackground = Color(0xFF201A0E),
        surface = Color(0xFFFFFCF5),
        onSurface = Color(0xFF201A0E),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFAF4E4),
        surfaceContainer = Color(0xFFF4ECD6),
        surfaceContainerHigh = Color(0xFFECE1C5),
      )

    AppPalette.BERRY_PLUM ->
      lightColorScheme(
        primary = Color(0xFF86198F),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFAE8FF),
        onPrimaryContainer = Color(0xFF4A044E),
        secondary = Color(0xFF059669),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD1FAE5),
        onSecondaryContainer = Color(0xFF064E3B),
        tertiary = Color(0xFFE11D48),
        onTertiary = Color.White,
        background = Color(0xFFFEF8FF),
        onBackground = Color(0xFF241126),
        surface = Color(0xFFFEF8FF),
        onSurface = Color(0xFF241126),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF9EEFB),
        surfaceContainer = Color(0xFFF3E2F7),
        surfaceContainerHigh = Color(0xFFECD3F2),
      )
  }

fun darkSchemeFor(palette: AppPalette): ColorScheme =
  when (palette) {
    AppPalette.OCEAN_TEAL ->
      darkColorScheme(
        primary = Color(0xFF67E8F9),
        onPrimary = Color(0xFF003642),
        primaryContainer = Color(0xFF0E7490),
        onPrimaryContainer = Color(0xFFCFFAFE),
        secondary = Color(0xFFFDBA74),
        onSecondary = Color(0xFF4A1800),
        secondaryContainer = Color(0xFF9A3412),
        onSecondaryContainer = Color(0xFFFFEDD5),
        tertiary = Color(0xFF5EEAD4),
        onTertiary = Color(0xFF003831),
        background = Color(0xFF0B1318),
        onBackground = Color(0xFFE2E8F0),
        surface = Color(0xFF0B1318),
        onSurface = Color(0xFFE2E8F0),
        surfaceContainerLowest = Color(0xFF070D11),
        surfaceContainerLow = Color(0xFF111C23),
        surfaceContainer = Color(0xFF17242D),
        surfaceContainerHigh = Color(0xFF1E2E39),
      )

    AppPalette.EMERALD_SAGE ->
      darkColorScheme(
        primary = Color(0xFF86EFAC),
        onPrimary = Color(0xFF003917),
        primaryContainer = Color(0xFF15803D),
        onPrimaryContainer = Color(0xFFDCFCE7),
        secondary = Color(0xFF5EEAD4),
        onSecondary = Color(0xFF003730),
        secondaryContainer = Color(0xFF115E59),
        onSecondaryContainer = Color(0xFFCCFBF1),
        background = Color(0xFF0B140E),
        onBackground = Color(0xFFE2EFE5),
        surface = Color(0xFF0B140E),
        onSurface = Color(0xFFE2EFE5),
        surfaceContainerLowest = Color(0xFF070E09),
        surfaceContainerLow = Color(0xFF121F16),
        surfaceContainer = Color(0xFF17271C),
        surfaceContainerHigh = Color(0xFF1F3225),
      )

    AppPalette.ROYAL_INDIGO ->
      darkColorScheme(
        primary = Color(0xFFA5B4FC),
        onPrimary = Color(0xFF1E1B4B),
        primaryContainer = Color(0xFF3730A3),
        onPrimaryContainer = Color(0xFFE0E7FF),
        secondary = Color(0xFFF472B6),
        onSecondary = Color(0xFF500724),
        secondaryContainer = Color(0xFF9D174D),
        onSecondaryContainer = Color(0xFFFCE7F3),
        background = Color(0xFF0D0E1B),
        onBackground = Color(0xFFE5E7F5),
        surface = Color(0xFF0D0E1B),
        onSurface = Color(0xFFE5E7F5),
        surfaceContainerLowest = Color(0xFF080912),
        surfaceContainerLow = Color(0xFF141628),
        surfaceContainer = Color(0xFF1A1C33),
        surfaceContainerHigh = Color(0xFF222542),
      )

    AppPalette.TERRACOTTA_CORAL ->
      darkColorScheme(
        primary = Color(0xFFFDBA74),
        onPrimary = Color(0xFF431407),
        primaryContainer = Color(0xFF9A3412),
        onPrimaryContainer = Color(0xFFFFEDD5),
        secondary = Color(0xFF7DD3FC),
        onSecondary = Color(0xFF082F49),
        secondaryContainer = Color(0xFF0369A1),
        onSecondaryContainer = Color(0xFFE0F2FE),
        background = Color(0xFF17100D),
        onBackground = Color(0xFFF3E7E1),
        surface = Color(0xFF17100D),
        onSurface = Color(0xFFF3E7E1),
        surfaceContainerLowest = Color(0xFF100B08),
        surfaceContainerLow = Color(0xFF211713),
        surfaceContainer = Color(0xFF2A1D18),
        surfaceContainerHigh = Color(0xFF34251F),
      )

    AppPalette.AMBER_GOLD ->
      darkColorScheme(
        primary = Color(0xFFFCD34D),
        onPrimary = Color(0xFF3B2400),
        primaryContainer = Color(0xFF92400E),
        onPrimaryContainer = Color(0xFFFEF3C7),
        secondary = Color(0xFF5EEAD4),
        onSecondary = Color(0xFF003730),
        secondaryContainer = Color(0xFF115E59),
        onSecondaryContainer = Color(0xFFCCFBF1),
        background = Color(0xFF15120A),
        onBackground = Color(0xFFF2ECE0),
        surface = Color(0xFF15120A),
        onSurface = Color(0xFFF2ECE0),
        surfaceContainerLowest = Color(0xFF0E0C06),
        surfaceContainerLow = Color(0xFF1F1B10),
        surfaceContainer = Color(0xFF282215),
        surfaceContainerHigh = Color(0xFF332C1C),
      )

    AppPalette.BERRY_PLUM ->
      darkColorScheme(
        primary = Color(0xFFF0ABFC),
        onPrimary = Color(0xFF4A044E),
        primaryContainer = Color(0xFF701A75),
        onPrimaryContainer = Color(0xFFFAE8FF),
        secondary = Color(0xFF6EE7B7),
        onSecondary = Color(0xFF022C22),
        secondaryContainer = Color(0xFF047857),
        onSecondaryContainer = Color(0xFFD1FAE5),
        background = Color(0xFF160D17),
        onBackground = Color(0xFFF2E6F3),
        surface = Color(0xFF160D17),
        onSurface = Color(0xFFF2E6F3),
        surfaceContainerLowest = Color(0xFF0F0810),
        surfaceContainerLow = Color(0xFF201322),
        surfaceContainer = Color(0xFF29192C),
        surfaceContainerHigh = Color(0xFF332037),
      )
  }
