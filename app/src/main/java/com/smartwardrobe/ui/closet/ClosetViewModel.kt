package com.smartwardrobe.ui.closet

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartwardrobe.data.local.entity.Category
import com.smartwardrobe.data.local.entity.ClothingItemEntity
import com.smartwardrobe.data.local.entity.Season
import com.smartwardrobe.domain.model.ClothingFilter
import com.smartwardrobe.domain.repository.WardrobeRepository
import com.smartwardrobe.domain.usecase.AddClothingPipeline
import com.smartwardrobe.domain.usecase.AddClothingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** 一次性 UI 事件（Toast / 导航）。 */
sealed interface ClosetEvent {
    data class Message(val text: String) : ClosetEvent
    data class Saved(val itemId: String) : ClosetEvent
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ClosetViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    val pipeline: AddClothingPipeline
) : ViewModel() {

    private val _filter = MutableStateFlow(ClothingFilter())
    val filter: StateFlow<ClothingFilter> = _filter.asStateFlow()

    val clothes: StateFlow<List<ClothingItemEntity>> = _filter
        .flatMapLatest { repository.observeClothes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<ClosetEvent>()
    val events: SharedFlow<ClosetEvent> = _events.asSharedFlow()

    // ---------- 筛选 ----------
    fun setCategory(category: Category?) = _filter.update { it.copy(category = category) }
    fun toggleSeason(season: Season) = _filter.update {
        val next = it.seasons.toMutableSet().apply { if (!add(season)) remove(season) }
        it.copy(seasons = next)
    }
    fun toggleStyle(style: String) = _filter.update {
        val next = it.styles.toMutableSet().apply { if (!add(style)) remove(style) }
        it.copy(styles = next)
    }
    fun setColorFamily(color: String?) = _filter.update { it.copy(colorFamily = color) }
    fun setKeyword(keyword: String) = _filter.update { it.copy(keyword = keyword) }
    fun clearFilter() { _filter.value = ClothingFilter() }

    // ---------- 录入流水线 ----------
    fun startAddFromPhoto(bitmap: Bitmap) = viewModelScope.launch { pipeline.process(bitmap) }

    fun confirmAdd(
        name: String,
        subCategory: String,
        category: Category,
        primaryColorName: String,
        primaryColorHex: String,
        seasons: List<Season>,
        styles: List<String>,
        material: String,
        warmthLevel: Int
    ) = viewModelScope.launch {
        val state = pipeline.state.value
        if (state !is AddClothingState.ReadyToConfirm) return@launch
        val entity = pipeline.buildEntity(
            imagePath = state.imagePath,
            analysis = state.analysis,
            name = name,
            subCategory = subCategory,
            category = category,
            primaryColorName = primaryColorName,
            primaryColorHex = primaryColorHex,
            seasons = seasons,
            styles = styles,
            material = material,
            warmthLevel = warmthLevel
        )
        repository.addClothing(entity)
        pipeline.reset()
        _events.emit(ClosetEvent.Saved(entity.id))
    }

    fun cancelAdd() = pipeline.reset()

    // ---------- 编辑 / 删除 / 穿着记录 ----------
    fun updateClothing(item: ClothingItemEntity) = viewModelScope.launch {
        repository.updateClothing(item)
        _events.emit(ClosetEvent.Message("已保存修改"))
    }

    fun deleteClothing(item: ClothingItemEntity) = viewModelScope.launch {
        repository.deleteClothing(item.id)
        _events.emit(ClosetEvent.Message("已删除：${item.name}"))
    }

    fun markWornToday(item: ClothingItemEntity) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
        repository.markWorn(item.id, now, dateKey)
        _events.emit(ClosetEvent.Message("已记录：今天穿了 ${item.name}"))
    }

}
