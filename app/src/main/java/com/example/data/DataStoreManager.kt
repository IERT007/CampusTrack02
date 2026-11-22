package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "caliper_datastore")

data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val lastSyncedAt: String = ""
)

class DataStoreManager(private val context: Context) {

    companion object {
        val KEY_SEMESTER_START_DATE = stringPreferencesKey("key_semester_start_date")
        val KEY_SEMESTER_END_DATE = stringPreferencesKey("key_semester_end_date")
        val KEY_THEME_PRESET = stringPreferencesKey("key_theme_preset")

        // Supabase Cloud Sync Session Keys
        val KEY_SUPABASE_ACCESS_TOKEN = stringPreferencesKey("key_supabase_access_token")
        val KEY_SUPABASE_REFRESH_TOKEN = stringPreferencesKey("key_supabase_refresh_token")
        val KEY_SUPABASE_USER_ID = stringPreferencesKey("key_supabase_user_id")
        val KEY_SUPABASE_USER_EMAIL = stringPreferencesKey("key_supabase_user_email")
        val KEY_SUPABASE_LAST_SYNCED_AT = stringPreferencesKey("key_supabase_last_synced_at")

        // OTA Updater Throttle Key
        val KEY_OTA_LAST_CHECK_MILLIS = longPreferencesKey("key_ota_last_check_millis")

        const val DEFAULT_START_DATE = "2026-09-19"
        const val DEFAULT_END_DATE = "2026-12-15"
        const val DEFAULT_THEME_PRESET = "obsidian_ice"
    }

    val semesterStartDateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SEMESTER_START_DATE] ?: DEFAULT_START_DATE
    }

    val semesterEndDateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SEMESTER_END_DATE] ?: DEFAULT_END_DATE
    }

    val themePresetFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_PRESET] ?: DEFAULT_THEME_PRESET
    }

    val supabaseSessionFlow: Flow<SupabaseSession?> = context.dataStore.data.map { prefs ->
        val token = prefs[KEY_SUPABASE_ACCESS_TOKEN].orEmpty()
        val userId = prefs[KEY_SUPABASE_USER_ID].orEmpty()
        val email = prefs[KEY_SUPABASE_USER_EMAIL].orEmpty()
        if (token.isNotBlank() && userId.isNotBlank()) {
            SupabaseSession(
                accessToken = token,
                refreshToken = prefs[KEY_SUPABASE_REFRESH_TOKEN].orEmpty(),
                userId = userId,
                email = email,
                lastSyncedAt = prefs[KEY_SUPABASE_LAST_SYNCED_AT].orEmpty()
            )
        } else {
            null
        }
    }

    val supabaseLastSyncedAtFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SUPABASE_LAST_SYNCED_AT].orEmpty()
    }

    val otaLastCheckMillisFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_OTA_LAST_CHECK_MILLIS] ?: 0L
    }

    suspend fun getSupabaseSessionDirect(): SupabaseSession? = supabaseSessionFlow.first()

    suspend fun getOtaLastCheckMillisDirect(): Long = otaLastCheckMillisFlow.first()

    suspend fun setSemesterDates(startDate: String, endDate: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SEMESTER_START_DATE] = startDate
            preferences[KEY_SEMESTER_END_DATE] = endDate
        }
    }

    suspend fun setSemesterStartDate(startDate: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SEMESTER_START_DATE] = startDate
        }
    }

    suspend fun setSemesterEndDate(endDate: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SEMESTER_END_DATE] = endDate
        }
    }

    suspend fun setThemePreset(presetKey: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_PRESET] = presetKey
        }
    }

    suspend fun saveSupabaseSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        email: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SUPABASE_ACCESS_TOKEN] = accessToken
            prefs[KEY_SUPABASE_REFRESH_TOKEN] = refreshToken
            prefs[KEY_SUPABASE_USER_ID] = userId
            prefs[KEY_SUPABASE_USER_EMAIL] = email
        }
    }

    suspend fun setSupabaseLastSyncedAt(isoTimestamp: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SUPABASE_LAST_SYNCED_AT] = isoTimestamp
        }
    }

    suspend fun clearSupabaseSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SUPABASE_ACCESS_TOKEN)
            prefs.remove(KEY_SUPABASE_REFRESH_TOKEN)
            prefs.remove(KEY_SUPABASE_USER_ID)
            prefs.remove(KEY_SUPABASE_USER_EMAIL)
        }
    }

    suspend fun setOtaLastCheckMillis(millis: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OTA_LAST_CHECK_MILLIS] = millis
        }
    }
}
