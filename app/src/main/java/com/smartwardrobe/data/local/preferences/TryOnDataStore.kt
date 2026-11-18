package com.smartwardrobe.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** 试衣间会话专用 DataStore（与用户偏好隔离）。 */
val Context.tryOnDataStore: DataStore<Preferences> by preferencesDataStore(name = "tryon_session")
