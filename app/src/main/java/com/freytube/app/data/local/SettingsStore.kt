package com.freytube.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    companion object {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val DEFAULT_REGION = stringPreferencesKey("default_region")
        val BACKGROUND_PLAY = booleanPreferencesKey("background_play")
        val SPONSORBLOCK_ENABLED = booleanPreferencesKey("sponsorblock_enabled")
        val PIPED_INSTANCE = stringPreferencesKey("piped_instance")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val AUTO_PLAY = booleanPreferencesKey("auto_play")
        val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        val DOWNLOAD_PATH = stringPreferencesKey("download_path")
        val PIP_ENABLED = booleanPreferencesKey("pip_enabled")
        val AMOLED_DARK = booleanPreferencesKey("amoled_dark")
    }

    val darkMode: Flow<Boolean> = context.settingsDataStore.data.map { it[DARK_MODE] ?: true }
    val amoledDark: Flow<Boolean> = context.settingsDataStore.data.map { it[AMOLED_DARK] ?: false }
    val defaultQuality: Flow<String> = context.settingsDataStore.data.map { it[DEFAULT_QUALITY] ?: "720p" }
    val defaultRegion: Flow<String> = context.settingsDataStore.data.map { it[DEFAULT_REGION] ?: "US" }
    val backgroundPlay: Flow<Boolean> = context.settingsDataStore.data.map { it[BACKGROUND_PLAY] ?: true }
    val sponsorBlockEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[SPONSORBLOCK_ENABLED] ?: true }
    val pipedInstance: Flow<String> = context.settingsDataStore.data.map { it[PIPED_INSTANCE] ?: "https://pipedapi.kavin.rocks/" }
    val playbackSpeed: Flow<Float> = context.settingsDataStore.data.map { it[PLAYBACK_SPEED] ?: 1.0f }
    val autoPlay: Flow<Boolean> = context.settingsDataStore.data.map { it[AUTO_PLAY] ?: true }
    val downloadQuality: Flow<String> = context.settingsDataStore.data.map { it[DOWNLOAD_QUALITY] ?: "720p" }
    val pipEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[PIP_ENABLED] ?: true }

    suspend fun <T> setSetting(key: Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { settings ->
            settings[key] = value
        }
    }
}
