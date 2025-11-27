package com.evanh.qlc_controller

import android.content.Context
import androidx.datastore.preferences.core.edit
class SettingsRepository(private val context: Context) {

    val settingsFlow = context.settingsDataStore.data

    suspend fun saveIp(ip: String) {
        context.settingsDataStore.edit { it[SettingsKeys.IP] = ip }
    }

    suspend fun savePort(port: Int) {
        context.settingsDataStore.edit { it[SettingsKeys.PORT] = port }
    }

    suspend fun saveUniverseCount(value: Int) {
        context.settingsDataStore.edit { it[SettingsKeys.UNIVERSE_COUNT] = value }
    }

    suspend fun saveDefaultUniverse(value: Int) {
        context.settingsDataStore.edit { it[SettingsKeys.DEFAULT_UNIVERSE] = value }
    }

    suspend fun savePageSize(value: Int) {
        context.settingsDataStore.edit { it[SettingsKeys.PAGE_SIZE] = value }
    }

    suspend fun saveDMXRefresh(value: Long) {
        context.settingsDataStore.edit { it[SettingsKeys.DMX_REFRESH] = value }
    }

    suspend fun saveDMXFade(value: Long) {
        context.settingsDataStore.edit { it[SettingsKeys.DMX_FADE] = value }
    }

    suspend fun saveIconLabels(value: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.ICON_LABELS] = value }
    }

    suspend fun saveHaptics(value: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.HAPTICS] = value }
    }
    suspend fun saveSettingsPopups(value: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.SETTINGS_POPUPS] = value }
    }
}
