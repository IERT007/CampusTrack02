package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "caliper_datastore")

class DataStoreManager(private val context: Context) {

    companion object {
        val KEY_SEMESTER_START_DATE = stringPreferencesKey("key_semester_start_date")
        val KEY_SEMESTER_END_DATE = stringPreferencesKey("key_semester_end_date")
        val KEY_THEME_PRESET = stringPreferencesKey("key_theme_preset")

        const val DEFAULT_START_DATE = "2026-07-15"
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
}
