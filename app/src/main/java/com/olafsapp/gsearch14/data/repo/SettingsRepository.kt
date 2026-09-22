package com.olafsapp.gsearch14.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.olafsapp.gsearch14.data.SearchEngine
import com.olafsapp.gsearch14.data.SearchEngineId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Seed colours used when Material You dynamic colour is off or unavailable. */
enum class AccentPalette { SIGNAL, FOREST, EMBER, INK, VIOLET }

/** Where a result page opens. */
enum class OpenTarget {
    /** The app's own reader-style WebView. */
    IN_APP,

    /** A Chrome Custom Tab — the user's browser engine, but still inside the app. */
    CUSTOM_TAB,

    /** Hand off to whatever browser the user has set as default. */
    EXTERNAL_BROWSER,
}

data class AppSettings(
    val engine: SearchEngine = SearchEngine.DEFAULT,
    val aiFree: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accent: AccentPalette = AccentPalette.SIGNAL,
    val pureBlackDark: Boolean = false,
    val openTarget: OpenTarget = OpenTarget.IN_APP,
    val suggestionsEnabled: Boolean = true,
    val incognito: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ENGINE = stringPreferencesKey("engine")
        val AI_FREE = booleanPreferencesKey("ai_free")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT = stringPreferencesKey("accent")
        val PURE_BLACK = booleanPreferencesKey("pure_black")
        val OPEN_TARGET = stringPreferencesKey("open_target")
        val SUGGESTIONS = booleanPreferencesKey("suggestions")
        val INCOGNITO = booleanPreferencesKey("incognito")
        val HAPTICS = booleanPreferencesKey("haptics")
        val BLOCK_COOKIES = booleanPreferencesKey("block_third_party_cookies")
        val MIGRATED = booleanPreferencesKey("migrated_legacy_settings")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { cause -> if (cause is IOException) emit(emptyPreferences()) else throw cause }
        .map { prefs ->
            AppSettings(
                engine = SearchEngine.byIdOrDefault(prefs[Keys.ENGINE]),
                aiFree = prefs[Keys.AI_FREE] ?: true,
                themeMode = prefs[Keys.THEME_MODE].toEnum(ThemeMode.SYSTEM),
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
                accent = prefs[Keys.ACCENT].toEnum(AccentPalette.SIGNAL),
                pureBlackDark = prefs[Keys.PURE_BLACK] ?: false,
                openTarget = prefs[Keys.OPEN_TARGET].toEnum(OpenTarget.IN_APP),
                suggestionsEnabled = prefs[Keys.SUGGESTIONS] ?: true,
                incognito = prefs[Keys.INCOGNITO] ?: false,
                hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
                blockThirdPartyCookies = prefs[Keys.BLOCK_COOKIES] ?: true,
            )
        }

    suspend fun setEngine(id: SearchEngineId) = edit { it[Keys.ENGINE] = id.name }
    suspend fun setAiFree(value: Boolean) = edit { it[Keys.AI_FREE] = value }
    suspend fun setThemeMode(value: ThemeMode) = edit { it[Keys.THEME_MODE] = value.name }
    suspend fun setDynamicColor(value: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = value }
    suspend fun setAccent(value: AccentPalette) = edit { it[Keys.ACCENT] = value.name }
    suspend fun setPureBlackDark(value: Boolean) = edit { it[Keys.PURE_BLACK] = value }
    suspend fun setOpenTarget(value: OpenTarget) = edit { it[Keys.OPEN_TARGET] = value.name }
    suspend fun setSuggestionsEnabled(value: Boolean) = edit { it[Keys.SUGGESTIONS] = value }
    suspend fun setIncognito(value: Boolean) = edit { it[Keys.INCOGNITO] = value }
    suspend fun setHapticsEnabled(value: Boolean) = edit { it[Keys.HAPTICS] = value }
    suspend fun setBlockThirdPartyCookies(value: Boolean) = edit { it[Keys.BLOCK_COOKIES] = value }

    /**
     * Carries the theme and browser choice over from the pre-4.0 SharedPreferences, so an
     * updating user keeps the setup they had.
     */
    suspend fun migrateLegacySettingsIfNeeded() {
        edit { prefs ->
            if (prefs[Keys.MIGRATED] == true) return@edit
            val legacy = context.getSharedPreferences("search_prefs", Context.MODE_PRIVATE)

            // AppCompatDelegate constants: 1 = MODE_NIGHT_NO, 2 = MODE_NIGHT_YES.
            when (legacy.getInt("night_mode", -1)) {
                1 -> prefs[Keys.THEME_MODE] = ThemeMode.LIGHT.name
                2 -> prefs[Keys.THEME_MODE] = ThemeMode.DARK.name
            }
            if (legacy.getString("browser_choice", "webview") == "external") {
                prefs[Keys.OPEN_TARGET] = OpenTarget.EXTERNAL_BROWSER.name
            }
            prefs[Keys.MIGRATED] = true
        }
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(fallback: T): T =
    runCatching { enumValueOf<T>(this ?: "") }.getOrDefault(fallback)
