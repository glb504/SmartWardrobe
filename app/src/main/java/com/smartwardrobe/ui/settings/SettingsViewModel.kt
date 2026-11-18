package com.smartwardrobe.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartwardrobe.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferences
) : ViewModel() {

    val apiKey: StateFlow<String> = preferences.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val bodyType: StateFlow<String> = preferences.bodyType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val skinTone: StateFlow<String> = preferences.skinTone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val darkMode: StateFlow<String> = preferences.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "system")

    fun setApiKey(value: String) = viewModelScope.launch { preferences.setApiKey(value) }
    fun setBodyType(value: String) = viewModelScope.launch { preferences.setBodyType(value) }
    fun setSkinTone(value: String) = viewModelScope.launch { preferences.setSkinTone(value) }
    fun setDarkMode(value: String) = viewModelScope.launch { preferences.setDarkMode(value) }
}
