package com.smartwardrobe.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "smartwardrobe_prefs")

/**
 * 用户偏好配置：API Key、体型肤色特征、默认场合等。
 * API Key 由用户在设置页填入，遵循运行时配置而非硬编码。
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val BODY_TYPE = stringPreferencesKey("body_type")
        val SKIN_TONE = stringPreferencesKey("skin_tone")
        val DEFAULT_OCCASION = stringPreferencesKey("default_occasion")
        val DARK_MODE = stringPreferencesKey("dark_mode")
    }

    val apiKey: Flow<String> = context.userDataStore.data.map { it[Keys.API_KEY] ?: "" }
    val bodyType: Flow<String> = context.userDataStore.data.map { it[Keys.BODY_TYPE] ?: "" }
    val skinTone: Flow<String> = context.userDataStore.data.map { it[Keys.SKIN_TONE] ?: "" }
    val defaultOccasion: Flow<String> = context.userDataStore.data.map { it[Keys.DEFAULT_OCCASION] ?: "日常" }
    val darkMode: Flow<String> = context.userDataStore.data.map { it[Keys.DARK_MODE] ?: "system" }

    suspend fun setApiKey(value: String) = context.userDataStore.edit { it[Keys.API_KEY] = value }
    suspend fun setBodyType(value: String) = context.userDataStore.edit { it[Keys.BODY_TYPE] = value }
    suspend fun setSkinTone(value: String) = context.userDataStore.edit { it[Keys.SKIN_TONE] = value }
    suspend fun setDefaultOccasion(value: String) = context.userDataStore.edit { it[Keys.DEFAULT_OCCASION] = value }
    suspend fun setDarkMode(value: String) = context.userDataStore.edit { it[Keys.DARK_MODE] = value }

    /** 一次性读取用户特征，供穿搭上下文组装。 */
    suspend fun snapshot(): StylistContextSnapshot = StylistContextSnapshot(
        bodyType = bodyType.first(),
        skinTone = skinTone.first()
    )
}

data class StylistContextSnapshot(val bodyType: String, val skinTone: String)
