package com.smartwardrobe.domain.usecase

import com.smartwardrobe.data.local.entity.ClothingItemEntity
import com.smartwardrobe.data.remote.PromptBuilder
import com.smartwardrobe.domain.model.ClothingFilter
import com.smartwardrobe.domain.model.StylistContext
import com.smartwardrobe.domain.repository.WardrobeRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 为穿搭 Agent 准备上下文：
 * 从衣橱中检索适合当前季节与温度的衣物，转成精简 JSON（不含图片）。
 */
@Singleton
class GetStylistWardrobe @Inject constructor(
    private val repository: WardrobeRepository,
    private val promptBuilder: PromptBuilder
) {
    suspend operator fun invoke(context: StylistContext): Pair<List<ClothingItemEntity>, String> {
        val items = first(repository.observeClothes(ClothingFilter()))
        val filtered = items.filter { matchesSeasonAndTemp(it, context) }
        val json = promptBuilder.serializeWardrobe(filtered)
        return filtered to json
    }

    private fun matchesSeasonAndTemp(item: ClothingItemEntity, ctx: StylistContext): Boolean {
        val seasonOk = ctx.season in item.seasons || item.seasons.isEmpty()
        // 温度高时排除过暖的衣物，温度低时排除过薄的衣物（留 1 级缓冲）
        val tempOk = when {
            ctx.temperatureC >= 28 -> item.warmthLevel <= 2
            ctx.temperatureC >= 20 -> item.warmthLevel <= 3
            ctx.temperatureC >= 12 -> item.warmthLevel in 2..4
            ctx.temperatureC >= 5 -> item.warmthLevel in 3..5
            else -> item.warmthLevel >= 4
        }
        return seasonOk && tempOk
    }

    private suspend fun <T> first(flow: kotlinx.coroutines.flow.Flow<T>): T = flow.first()
}
