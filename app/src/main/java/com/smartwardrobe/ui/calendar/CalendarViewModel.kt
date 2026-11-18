package com.smartwardrobe.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartwardrobe.data.local.entity.ClothingItemEntity
import com.smartwardrobe.data.local.entity.WearRecordEntity
import com.smartwardrobe.domain.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    repository: WardrobeRepository
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 最近 30 天记录。 */
    val records: StateFlow<List<WearRecordEntity>> = repository.observeWearRecordsByRange(
        dateFormat.format(Date(System.currentTimeMillis() - 30L * 24 * 3600 * 1000)),
        dateFormat.format(Date())
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val clothes: StateFlow<List<ClothingItemEntity>> =
        repository.observeClothes(com.smartwardrobe.domain.model.ClothingFilter())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 各衣物穿着次数排行（Top N）。 */
    val ranking: StateFlow<List<Pair<ClothingItemEntity, Int>>> = combine(clothes, records) { items, recs ->
        val counts = recs.filter { it.targetType == com.smartwardrobe.data.local.entity.WearTargetType.ITEM }
            .groupingBy { it.targetId }.eachCount()
        items.map { it to (counts[it.id] ?: 0) }
            .sortedByDescending { it.second }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
