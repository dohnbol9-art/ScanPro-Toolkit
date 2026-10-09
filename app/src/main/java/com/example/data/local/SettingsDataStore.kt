package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "scanpro_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "DARK", "LIGHT"
        val SCAN_VIBRATION = booleanPreferencesKey("scan_vibration")
        val SCAN_BEEP = booleanPreferencesKey("scan_beep")
        val AUTO_COPY = booleanPreferencesKey("auto_copy")
        val BATCH_MODE = booleanPreferencesKey("batch_mode")
        val SAFE_URL_PREVIEW = booleanPreferencesKey("safe_url_preview")
        val BLOCK_DANGEROUS_SCHEMES = booleanPreferencesKey("block_dangerous_schemes")
        val DEFAULT_ECC = stringPreferencesKey("default_ecc") // "M", "H", "L", "Q"
        val DEFAULT_RESOLUTION = intPreferencesKey("default_resolution") // 512, 1024, 2048
        val HISTORY_RETENTION_DAYS = intPreferencesKey("history_retention_days") // -1 (forever), 30, 90
    }

    val themeMode: Flow<String> = context.dataStore.data.map { it[Keys.THEME_MODE] ?: "SYSTEM" }
    val scanVibration: Flow<Boolean> = context.dataStore.data.map { it[Keys.SCAN_VIBRATION] ?: true }
    val scanBeep: Flow<Boolean> = context.dataStore.data.map { it[Keys.SCAN_BEEP] ?: false }
    val autoCopy: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_COPY] ?: false }
    val batchMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.BATCH_MODE] ?: false }
    val safeUrlPreview: Flow<Boolean> = context.dataStore.data.map { it[Keys.SAFE_URL_PREVIEW] ?: true }
    val blockDangerousSchemes: Flow<Boolean> = context.dataStore.data.map { it[Keys.BLOCK_DANGEROUS_SCHEMES] ?: true }
    val defaultEcc: Flow<String> = context.dataStore.data.map { it[Keys.DEFAULT_ECC] ?: "H" }
    val defaultResolution: Flow<Int> = context.dataStore.data.map { it[Keys.DEFAULT_RESOLUTION] ?: 1024 }
    val historyRetentionDays: Flow<Int> = context.dataStore.data.map { it[Keys.HISTORY_RETENTION_DAYS] ?: -1 }

    suspend fun setThemeMode(mode: String) = context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    suspend fun setScanVibration(enabled: Boolean) = context.dataStore.edit { it[Keys.SCAN_VIBRATION] = enabled }
    suspend fun setScanBeep(enabled: Boolean) = context.dataStore.edit { it[Keys.SCAN_BEEP] = enabled }
    suspend fun setAutoCopy(enabled: Boolean) = context.dataStore.edit { it[Keys.AUTO_COPY] = enabled }
    suspend fun setBatchMode(enabled: Boolean) = context.dataStore.edit { it[Keys.BATCH_MODE] = enabled }
    suspend fun setSafeUrlPreview(enabled: Boolean) = context.dataStore.edit { it[Keys.SAFE_URL_PREVIEW] = enabled }
    suspend fun setBlockDangerousSchemes(enabled: Boolean) = context.dataStore.edit { it[Keys.BLOCK_DANGEROUS_SCHEMES] = enabled }
    suspend fun setDefaultEcc(ecc: String) = context.dataStore.edit { it[Keys.DEFAULT_ECC] = ecc }
    suspend fun setDefaultResolution(res: Int) = context.dataStore.edit { it[Keys.DEFAULT_RESOLUTION] = res }
    suspend fun setHistoryRetentionDays(days: Int) = context.dataStore.edit { it[Keys.HISTORY_RETENTION_DAYS] = days }
}
