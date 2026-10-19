package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.caliperDataStore: DataStore<Preferences> by preferencesDataStore(name = "caliper_preferences")

class DataStoreManager(private val context: Context) {

    companion object {
        val KEY_SEMESTER_START_DATE = stringPreferencesKey("semester_start_date")
        val KEY_SEMESTER_END_DATE = stringPreferencesKey("semester_end_date")
        val KEY_MONOCHROME_MODE = booleanPreferencesKey("monochrome_mode")
        val KEY_SELECTED_THEME = stringPreferencesKey("selected_theme")
        val KEY_24_HOUR_FORMAT = booleanPreferencesKey("24_hour_format")
        val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val KEY_AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
        const val DEFAULT_START_DATE = "2026-01-19"
        const val DEFAULT_END_DATE = "2026-05-15"
    }

    val semesterStartDateFlow: Flow<String> = context.caliperDataStore.data.map { prefs ->
        prefs[KEY_SEMESTER_START_DATE] ?: DEFAULT_START_DATE
    }

    val semesterEndDateFlow: Flow<String> = context.caliperDataStore.data.map { prefs ->
        prefs[KEY_SEMESTER_END_DATE] ?: DEFAULT_END_DATE
    }

    val isMonochromeFlow: Flow<Boolean> = context.caliperDataStore.data.map { prefs ->
        prefs[KEY_MONOCHROME_MODE] ?: false
    }

    val selectedThemeFlow: Flow<String> = context.caliperDataStore.data.map { prefs ->
        prefs[KEY_SELECTED_THEME] ?: "Obsidian"
    }

    val is24HourFormatFlow: Flow<Boolean> = context.caliperDataStore.data.map { prefs ->
        prefs[KEY_24_HOUR_FORMAT] ?: false
    }

    val isHapticsEnabledFlow: Flow<Boolean> = context.caliperDataStore.data.map { prefs ->
        prefs[KEY_HAPTICS_ENABLED] ?: true
    }

    val isAudioEnabledFlow: Flow<Boolean> = context.caliperDataStore.data.map { prefs ->
        prefs[KEY_AUDIO_ENABLED] ?: true
    }

    suspend fun setSemesterStartDate(startDateIso: String) {
        context.caliperDataStore.edit { prefs ->
            prefs[KEY_SEMESTER_START_DATE] = startDateIso
        }
    }

    suspend fun setSemesterEndDate(endDateIso: String) {
        context.caliperDataStore.edit { prefs ->
            prefs[KEY_SEMESTER_END_DATE] = endDateIso
        }
    }

    suspend fun setSemesterDates(startDateIso: String, endDateIso: String) {
        context.caliperDataStore.edit { prefs ->
            prefs[KEY_SEMESTER_START_DATE] = startDateIso
            prefs[KEY_SEMESTER_END_DATE] = endDateIso
        }
    }

    suspend fun setMonochromeMode(enabled: Boolean) {
        context.caliperDataStore.edit { prefs ->
            prefs[KEY_MONOCHROME_MODE] = enabled
        }
    }

    suspend fun setSelectedTheme(theme: String) {
        context.caliperDataStore.edit { prefs ->
            prefs[KEY_SELECTED_THEME] = theme
        }
    }

    suspend fun set24HourFormat(enabled: Boolean) {
        context.caliperDataStore.edit { prefs ->
            prefs[KEY_24_HOUR_FORMAT] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.caliperDataStore.edit { prefs ->
            prefs[KEY_HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setAudioEnabled(enabled: Boolean) {
        context.caliperDataStore.edit { prefs ->
            prefs[KEY_AUDIO_ENABLED] = enabled
        }
    }
}
