package com.smartwardrobe.ui.tryon

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartwardrobe.data.image.ImageFileManager
import com.smartwardrobe.data.image.OutfitCanvasComposer
import com.smartwardrobe.data.image.PortraitSegmentationHelper
import com.smartwardrobe.domain.model.LayerTransform
import com.smartwardrobe.domain.model.TryOnSession
import com.smartwardrobe.domain.repository.TryOnRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface TryOnUiState {
    data object Idle : TryOnUiState
    data class Extracting(val progress: Float = 0f) : TryOnUiState
    data class Ready(val session: TryOnSession, val preview: Bitmap?) : TryOnUiState
    data class Error(val message: String) : TryOnUiState
}

sealed interface TryOnEvent {
    data class Message(val text: String) : TryOnEvent
}

@HiltViewModel
class TryOnViewModel @Inject constructor(
    private val portraitHelper: PortraitSegmentationHelper,
    private val repository: TryOnRepository,
    private val composer: OutfitCanvasComposer,
    private val fileManager: ImageFileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<TryOnUiState>(TryOnUiState.Idle)
    val uiState: StateFlow<TryOnUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TryOnEvent>()
    val events: SharedFlow<TryOnEvent> = _events.asSharedFlow()

    init {
        // 恢复上次会话
        viewModelScope.launch {
            repository.getSession()?.let { session -> refresh(session) }
        }
    }

    /** 拍照/选图后提取真人透明形象。 */
    fun extractPortrait(source: Bitmap) = viewModelScope.launch {
        _uiState.value = TryOnUiState.Extracting(0.2f)
        try {
            val path = portraitHelper.extractPortraitAndSave(source)
            val session = com.smartwardrobe.domain.model.TryOnSession(
                portraitPath = path,
                outfitTitle = "自由搭配"
            )
            // 持久化人像路径
            repository.savePortrait(path)
            refresh(session)
            _events.emit(TryOnEvent.Message("真人形象已提取，可以开始试衣"))
        } catch (e: Exception) {
            _uiState.value = TryOnUiState.Error(e.message ?: "人像提取失败")
        }
    }

    /** 一键套用 AI 搭配：把搭配中的衣物映射为试衣图层。 */
    fun applyOutfit(itemIds: List<String>, title: String) = viewModelScope.launch {
        val session = repository.buildSessionFromItems(itemIds, title)
        if (session.portraitPath.isBlank()) {
            _events.emit(TryOnEvent.Message("请先拍照录入真人形象"))
            return@launch
        }
        refresh(session)
        _events.emit(TryOnEvent.Message("已套用搭配：${title}"))
    }

    /** 用户自行换装：替换某个部位槽位的衣物。 */
    fun changeSlot(slot: String, itemId: String?) = viewModelScope.launch {
        val session = repository.replaceSlot(slot, itemId)
        refresh(session)
    }

    /** 拖拽/缩放/旋转某图层。 */
    fun updateTransform(slot: String, transform: LayerTransform) = viewModelScope.launch {
        val session = repository.updateTransform(slot, transform)
        refresh(session)
    }

    fun clear() = viewModelScope.launch {
        repository.clearSession()
        _uiState.value = TryOnUiState.Idle
    }

    /** 依据会话合成预览图（IO 密集，切到 Default）。 */
    private suspend fun refresh(session: TryOnSession) {
        val preview = if (session.portraitPath.isNotBlank()) {
            withContext(Dispatchers.Default) {
                composer.composeTryOn(session.portraitPath, session.layers)
            }
        } else null
        _uiState.value = TryOnUiState.Ready(session, preview)
    }
}
