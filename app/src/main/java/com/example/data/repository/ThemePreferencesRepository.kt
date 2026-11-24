package com.example.data.repository

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppPalette
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "otter_theme_prefs")

data class ThemePreferences(
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val useDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
  val palette: AppPalette = AppPalette.OCEAN_TEAL,
)

class ThemePreferencesRepository(private val context: Context) {
  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
    val PALETTE = stringPreferencesKey("accent_palette")
  }

  val preferencesFlow: Flow<ThemePreferences> =
    context.themeDataStore.data.map { prefs ->
      val mode =
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
          ?: ThemeMode.SYSTEM
      val dynamicDefault = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
      val useDynamic = prefs[Keys.DYNAMIC_COLOR] ?: dynamicDefault
      val palette =
        prefs[Keys.PALETTE]?.let { runCatching { AppPalette.valueOf(it) }.getOrNull() }
          ?: AppPalette.OCEAN_TEAL
      ThemePreferences(themeMode = mode, useDynamicColor = useDynamic, palette = palette)
    }

  suspend fun getSnapshot(): ThemePreferences = preferencesFlow.first()

  suspend fun setThemeMode(mode: ThemeMode) {
    context.themeDataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setUseDynamicColor(enabled: Boolean) {
    context.themeDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
  }

  suspend fun setPalette(palette: AppPalette) {
    context.themeDataStore.edit { it[Keys.PALETTE] = palette.name }
  }

  suspend fun restorePreferences(prefs: ThemePreferences) {
    context.themeDataStore.edit {
      it[Keys.THEME_MODE] = prefs.themeMode.name
      it[Keys.DYNAMIC_COLOR] = prefs.useDynamicColor
      it[Keys.PALETTE] = prefs.palette.name
    }
  }
}
