package com.smartwardrobe.domain.usecase

import android.graphics.Bitmap
import com.smartwardrobe.data.image.BackgroundRemoverHelper
import com.smartwardrobe.data.image.ImageFileManager
import com.smartwardrobe.data.local.entity.ClothingItemEntity
import com.smartwardrobe.domain.model.ClothingAnalysis
import com.smartwardrobe.domain.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 衣物录入流水线状态机。 */
sealed interface AddClothingState {
    data object Idle : AddClothingState
    data class RemovingBackground(val progress: Float = 0f) : AddClothingState
    data class Analyzing(val imagePath: String) : AddClothingState
    data class ReadyToConfirm(val imagePath: String, val analysis: ClothingAnalysis) : AddClothingState
    data class Saving(val imagePath: String, val analysis: ClothingAnalysis) : AddClothingState
    data class Saved(val itemId: String) : AddClothingState
    data class Error(val message: String, val stage: String) : AddClothingState
}

/**
 * 智能衣物录入编排：
 * 1) 端侧抠图 -> 透明 PNG
 * 2) 视觉 AI 打标 -> 结构化属性
 * 3) 停在 ReadyToConfirm 等待用户微调（UI 层消费此状态填充表单）
 */
@Singleton
class AddClothingPipeline @Inject constructor(
    private val backgroundRemover: BackgroundRemoverHelper,
    private val imageFileManager: ImageFileManager,
    private val aiRepository: AiRepository
) {
    private val _state = MutableStateFlow<AddClothingState>(AddClothingState.Idle)
    val state: StateFlow<AddClothingState> = _state.asStateFlow()

    /** 步骤 1+2：从原始照片抠图并打标，产出待确认数据。 */
    suspend fun process(source: Bitmap): AddClothingState {
        return try {
            _state.value = AddClothingState.RemovingBackground(0.1f)
            val imagePath = backgroundRemover.removeBackgroundAndSave(source)
            _state.value = AddClothingState.Analyzing(imagePath)
            val analysis = try {
                aiRepository.analyzeClothing(imagePath)
            } catch (e: Exception) {
                // AI 失败不阻塞录入：给出中性默认值，用户仍可手动完善
                ClothingAnalysis(
                    name = "未识别衣物",
                    category = com.smartwardrobe.data.local.entity.Category.TOP_INNER,
                    subCategory = "",
                    primaryColor = "未知色",
                    primaryColorHex = "#CCCCCC"
                )
            }
            val ready = AddClothingState.ReadyToConfirm(imagePath, analysis)
            _state.value = ready
            ready
        } catch (e: Exception) {
            val error = AddClothingState.Error(e.message ?: "抠图失败", "抠图")
            _state.value = error
            error
        }
    }

    /** 由 analysis + 用户微调后的字段组装实体（供 ViewModel 入库）。 */
    fun buildEntity(
        imagePath: String,
        analysis: ClothingAnalysis,
        name: String,
        subCategory: String,
        category: com.smartwardrobe.data.local.entity.Category,
        primaryColorName: String,
        primaryColorHex: String,
        seasons: List<com.smartwardrobe.data.local.entity.Season>,
        styles: List<String>,
        material: String,
        warmthLevel: Int
    ): ClothingItemEntity = ClothingItemEntity(
        imagePath = imagePath,
        name = name.ifBlank { analysis.name },
        category = category,
        subCategory = subCategory.ifBlank { analysis.subCategory },
        primaryColor = ClothingItemEntity.buildPrimaryColor(
            primaryColorName.ifBlank { analysis.primaryColor },
            primaryColorHex.ifBlank { analysis.primaryColorHex }
        ),
        seasons = seasons.ifEmpty { analysis.seasons },
        styles = styles.ifEmpty { analysis.styles },
        material = material.ifBlank { analysis.material },
        warmthLevel = warmthLevel.coerceIn(1, 5)
    )

    fun reset() { _state.value = AddClothingState.Idle }
}
