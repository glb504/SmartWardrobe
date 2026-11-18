package com.smartwardrobe.domain.repository

import com.smartwardrobe.domain.model.ClothingAnalysis
import com.smartwardrobe.domain.model.StylistContext
import com.smartwardrobe.domain.model.StylistSuggestion

/** AI 能力契约：视觉打标 + 智能穿搭生成。 */
interface AiRepository {
    /** 输入抠图后 PNG 的绝对路径，输出结构化衣物属性。 */
    suspend fun analyzeClothing(imagePath: String): ClothingAnalysis

    /** 输入上下文与精简衣物清单，输出 2-3 套穿搭建议。 */
    suspend fun generateOutfits(
        context: StylistContext,
        wardrobeJson: String
    ): List<StylistSuggestion>
}
