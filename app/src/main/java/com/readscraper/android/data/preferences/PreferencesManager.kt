package com.readscraper.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "read_scraper_prefs")

class PreferencesManager(context: Context) {
    private val dataStore = context.dataStore
    
    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val API_URL = stringPreferencesKey("api_url")
    }
    
    val apiKey: Flow<String?> = dataStore.data.map { preferences ->
        preferences[API_KEY]
    }
    
    val apiUrl: Flow<String?> = dataStore.data.map { preferences ->
        preferences[API_URL] ?: "http://104.244.74.191"
    }
    
    suspend fun saveApiKey(key: String) {
        dataStore.edit { preferences ->
            preferences[API_KEY] = key
        }
    }
    
    suspend fun saveApiUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[API_URL] = url
        }
    }
}

