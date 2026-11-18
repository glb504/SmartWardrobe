package com.smartwardrobe.ui.stylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartwardrobe.data.local.entity.OutfitEntity
import com.smartwardrobe.data.local.entity.Season
import com.smartwardrobe.data.local.preferences.UserPreferences
import com.smartwardrobe.domain.model.StylistContext
import com.smartwardrobe.domain.repository.TryOnRepository
import com.smartwardrobe.domain.repository.WardrobeRepository
import com.smartwardrobe.domain.usecase.GenerateOutfits
import com.smartwardrobe.domain.usecase.GetStylistWardrobe
import com.smartwardrobe.domain.usecase.ResolvedOutfit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StylistUiState {
    data object Idle : StylistUiState
    data class Loading(val step: String) : StylistUiState
    data class Success(val outfits: List<ResolvedOutfit>) : StylistUiState
    data class Error(val message: String) : StylistUiState
}

sealed interface StylistEvent {
    data class Message(val text: String) : StylistEvent
}

@HiltViewModel
class StylistViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val getStylistWardrobe: GetStylistWardrobe,
    private val generateOutfits: GenerateOutfits,
    private val preferences: UserPreferences,
    private val tryOnRepository: TryOnRepository
) : ViewModel() {

    private val _context = MutableStateFlow(
        StylistContext(
            occasion = "通勤",
            temperatureC = 18,
            weather = "多云",
            season = Season.AUTUMN
        )
    )
    val context: StateFlow<StylistContext> = _context.asStateFlow()

    private val _uiState = MutableStateFlow<StylistUiState>(StylistUiState.Idle)
    val uiState: StateFlow<StylistUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StylistEvent>()
    val events: SharedFlow<StylistEvent> = _events.asSharedFlow()

    val history: StateFlow<List<OutfitEntity>> = repository.observeOutfits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<OutfitEntity>> = repository.observeFavoriteOutfits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------- 上下文收集 ----------
    fun setOccasion(value: String) = _context.update { it.copy(occasion = value) }
    fun setTemperature(value: Int) = _context.update {
        it.copy(temperatureC = value, season = inferSeason(value))
    }
    fun setWeather(value: String) = _context.update { it.copy(weather = value) }
    fun setSeason(value: Season) = _context.update { it.copy(season = value) }
    fun setMood(value: String) = _context.update { it.copy(moodOrRequest = value) }

    /** 生成搭配（含用户体型肤色）。 */
    fun generate() = viewModelScope.launch {
        _uiState.value = StylistUiState.Loading("正在检索衣橱...")
        val snapshot = preferences.snapshot()
        val ctx = _context.value.copy(
            bodyType = snapshot.bodyType,
            skinTone = snapshot.skinTone
        )
        try {
            val (_, wardrobeJson) = getStylistWardrobe(ctx)
            if (wardrobeJson == "[]") {
                _uiState.value = StylistUiState.Error("没有符合当前季节/温度的衣物，请先调整条件或添加衣物")
                return@launch
            }
            _uiState.value = StylistUiState.Loading("AI 正在构思搭配...")
            val outfits = generateOutfits(wardrobeJson, ctx)
            if (outfits.isEmpty()) {
                _uiState.value = StylistUiState.Error("AI 未返回有效搭配，请重试或修改要求")
            } else {
                _uiState.value = StylistUiState.Success(outfits)
            }
        } catch (e: Exception) {
            _uiState.value = StylistUiState.Error(e.message ?: "生成失败，请检查 API Key 与网络")
        }
    }

    /** 换一批：在原有上下文基础上重跑，AI 采样会给出不同结果。 */
    fun regenerate() = generate()

    fun toggleFavorite(outfit: OutfitEntity) = viewModelScope.launch {
        repository.setOutfitFavorite(outfit.id, !outfit.isFavorite)
        _events.emit(StylistEvent.Message(if (!outfit.isFavorite) "已收藏搭配" else "已取消收藏"))
    }

    fun deleteOutfit(outfit: OutfitEntity) = viewModelScope.launch {
        repository.deleteOutfit(outfit.id)
        _events.emit(StylistEvent.Message("已删除搭配"))
    }

    /** 把某套搭配写入试衣会话，供试衣间页恢复后一键套用。 */
    fun prepareTryOn(outfit: OutfitEntity) = viewModelScope.launch {
        tryOnRepository.buildSessionFromItems(outfit.clothingItemIds, outfit.title)
        _events.emit(StylistEvent.Message("已送入试衣间"))
    }

    private fun inferSeason(temp: Int): Season = when {
        temp >= 28 -> Season.SUMMER
        temp >= 18 -> Season.SPRING
        temp >= 10 -> Season.AUTUMN
        else -> Season.WINTER
    }
}
