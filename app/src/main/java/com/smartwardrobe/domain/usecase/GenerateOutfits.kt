package com.smartwardrobe.domain.usecase

import com.smartwardrobe.data.local.entity.ClothingItemEntity
import com.smartwardrobe.data.local.entity.OutfitEntity
import com.smartwardrobe.domain.model.StylistSuggestion
import com.smartwardrobe.domain.repository.AiRepository
import com.smartwardrobe.domain.repository.WardrobeRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 已解析并映射好本地衣物的搭配结果。 */
data class ResolvedOutfit(
    val entity: OutfitEntity,
    val items: List<ClothingItemEntity>
)

/**
 * 生成穿搭：
 * 调 AI 拿到 {title, selectedItemIds, ...}，校验 ID 必须是衣橱中真实存在的，
 * 映射回本地实体并落库。
 */
@Singleton
class GenerateOutfits @Inject constructor(
    private val aiRepository: AiRepository,
    private val wardrobeRepository: WardrobeRepository
) {
    suspend operator fun invoke(
        wardrobeJson: String,
        context: com.smartwardrobe.domain.model.StylistContext
    ): List<ResolvedOutfit> {
        val suggestions: List<StylistSuggestion> = aiRepository.generateOutfits(context, wardrobeJson)
        val resolved = suggestions.mapNotNull { suggestion ->
            val items = wardrobeRepository.getClothesByIds(suggestion.selectedItemIds)
            // 只保留所有 ID 都能映射成功的方案，杜绝"幽灵衣物"
            if (items.size != suggestion.selectedItemIds.size || items.isEmpty()) return@mapNotNull null
            val entity = OutfitEntity(
                title = suggestion.title,
                clothingItemIds = suggestion.selectedItemIds,
                occasion = context.occasion,
                targetTempRange = suggestion.targetTempRange.ifBlank { "${context.temperatureC - 3}°C - ${context.temperatureC + 3}°C" },
                aiReasoning = suggestion.aiReasoning,
                stylingTips = suggestion.stylingTips
            )
            ResolvedOutfit(entity, items)
        }
        if (resolved.isNotEmpty()) wardrobeRepository.saveOutfits(resolved.map { it.entity })
        return resolved
    }
}
