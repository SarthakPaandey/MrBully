package com.brutal.accountability.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "prefs")

class AppPrefs(private val context: Context) {
    private val strictMode = booleanPreferencesKey("strict_mode")
    private val accountabilityPhrase = stringPreferencesKey("accountability_phrase")
    private val groqApiKeyEncrypted = stringPreferencesKey("groq_api_key_encrypted")
    private val groqApiKey = stringPreferencesKey("groq_api_key")

    val strictModeFlow: Flow<Boolean> = context.dataStore.data.map { it[strictMode] ?: true }
    val accountabilityPhraseFlow: Flow<String> = context.dataStore.data.map {
        it[accountabilityPhrase] ?: "I choose discipline over distraction"
    }
    val apiKeyFlow: Flow<String> = context.dataStore.data.map {
        val encrypted = it[groqApiKeyEncrypted].orEmpty()
        val legacyPlain = it[groqApiKey].orEmpty()
        when {
            encrypted.isNotBlank() -> ApiKeyCipher.decrypt(encrypted)
            legacyPlain.isNotBlank() -> legacyPlain
            else -> ""
        }
    }

    suspend fun setStrictMode(enabled: Boolean) {
        context.dataStore.edit { it[strictMode] = enabled }
    }

    suspend fun setPhrase(phrase: String) {
        context.dataStore.edit { it[accountabilityPhrase] = phrase }
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit {
            if (key.isBlank()) {
                it[groqApiKeyEncrypted] = ""
                it[groqApiKey] = ""
            } else {
                it[groqApiKeyEncrypted] = ApiKeyCipher.encrypt(key)
                it[groqApiKey] = ""
            }
        }
    }
}
