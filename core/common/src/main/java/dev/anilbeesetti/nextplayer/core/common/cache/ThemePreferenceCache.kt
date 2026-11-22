package dev.anilbeesetti.nextplayer.core.common.cache

import android.content.Context

/**
 * Tiny synchronous SharedPreferences mirror for theme settings so MainActivity.onCreate()
 * knows the exact dark/light mode before Compose renders frame 0 — preventing any startup flash.
 */
object ThemePreferenceCache {
    private const val PREFS_NAME = "afterplayer_theme_sync_cache"
    private const val KEY_THEME_CONFIG = "theme_config"
    private const val KEY_HIGH_CONTRAST = "high_contrast_dark"
    private const val KEY_DYNAMIC_COLORS = "dynamic_colors"

    fun save(
        context: Context,
        themeConfigName: String,
        highContrastDark: Boolean,
        dynamicColors: Boolean,
    ) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_CONFIG, themeConfigName)
            .putBoolean(KEY_HIGH_CONTRAST, highContrastDark)
            .putBoolean(KEY_DYNAMIC_COLORS, dynamicColors)
            .apply()
    }

    fun getThemeConfigName(context: Context): String =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_CONFIG, "SYSTEM") ?: "SYSTEM"

    fun getHighContrastDark(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HIGH_CONTRAST, false)

    fun getDynamicColors(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DYNAMIC_COLORS, false)
}
