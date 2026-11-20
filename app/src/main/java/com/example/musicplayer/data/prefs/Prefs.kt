package com.example.musicplayer.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class SortOrder { TITLE, ARTIST, ALBUM, RECENT, MOST_PLAYED }

/** Estado persistido do equalizador. presetIndex -1 = Personalizado; lista vazia = tudo 0 mB. */
data class EqSettings(
    val enabled: Boolean = false,
    val presetIndex: Int = -1,
    val bandLevelsMb: List<Int> = emptyList()
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Preferências persistidas (DataStore). Usa apenas o applicationContext. */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    private val data: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val themeMode: Flow<ThemeMode> = data
        .map { p -> p[KEY_THEME].toEnumOrDefault(ThemeMode.DARK) }
        .distinctUntilChanged()

    val sortOrder: Flow<SortOrder> = data
        .map { p -> p[KEY_SORT].toEnumOrDefault(SortOrder.TITLE) }
        .distinctUntilChanged()

    val eqSettings: Flow<EqSettings> = data.map { p ->
        EqSettings(
            enabled = p[KEY_EQ_ENABLED] ?: false,
            presetIndex = p[KEY_EQ_PRESET] ?: -1,
            bandLevelsMb = p[KEY_EQ_LEVELS].orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }
        )
    }.distinctUntilChanged()

    /** Tempo total efetivamente ouvido (ms), acumulado pelo PlaybackService. */
    val totalListenedMs: Flow<Long> = data
        .map { p -> p[KEY_TOTAL_LISTENED] ?: 0L }
        .distinctUntilChanged()

    /** Soma [deltaMs] ao total ouvido (atômico via edit). */
    suspend fun addListenedMs(deltaMs: Long) {
        if (deltaMs <= 0) return
        store.edit { it[KEY_TOTAL_LISTENED] = (it[KEY_TOTAL_LISTENED] ?: 0L) + deltaMs }
    }

    /** Ids (Track.id) das faixas favoritas. */
    val favoriteIds: Flow<Set<Long>> = data
        .map { p -> p[KEY_FAVORITES].orEmpty().mapNotNull { it.toLongOrNull() }.toSet() }
        .distinctUntilChanged()

    suspend fun toggleFavorite(trackId: Long) {
        store.edit {
            val cur = it[KEY_FAVORITES].orEmpty()
            val k = trackId.toString()
            it[KEY_FAVORITES] = if (k in cur) cur - k else cur + k
        }
    }

    suspend fun setEqSettings(s: EqSettings) {
        store.edit {
            it[KEY_EQ_ENABLED] = s.enabled
            it[KEY_EQ_PRESET] = s.presetIndex
            it[KEY_EQ_LEVELS] = s.bandLevelsMb.joinToString(",")
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setSortOrder(order: SortOrder) {
        store.edit { it[KEY_SORT] = order.name }
    }

    /** Leitura única dos valores atuais (para valor inicial coerente). */
    suspend fun readInitial(): Pair<ThemeMode, SortOrder> =
        themeMode.first() to sortOrder.first()

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_SORT = stringPreferencesKey("sort_order")
        val KEY_TOTAL_LISTENED = longPreferencesKey("total_listened_ms")
        val KEY_FAVORITES = stringSetPreferencesKey("favorite_track_ids")
        val KEY_EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val KEY_EQ_PRESET = intPreferencesKey("eq_preset_index")
        val KEY_EQ_LEVELS = stringPreferencesKey("eq_band_levels_mb")
    }
}

private inline fun <reified E : Enum<E>> String?.toEnumOrDefault(default: E): E =
    enumValues<E>().firstOrNull { it.name == this } ?: default
