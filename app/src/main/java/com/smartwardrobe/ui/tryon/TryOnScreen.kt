package com.smartwardrobe.ui.tryon

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import com.smartwardrobe.data.local.entity.Category
import com.smartwardrobe.data.local.entity.ClothingItemEntity
import com.smartwardrobe.domain.model.LayerTransform
import com.smartwardrobe.domain.model.TryOnSlots
import com.smartwardrobe.domain.repository.WardrobeRepository
import com.smartwardrobe.ui.common.CameraXPreview
import com.smartwardrobe.ui.common.imageOnlyRequest
import com.smartwardrobe.ui.common.createImageFile
import com.smartwardrobe.ui.common.rememberPickImageLauncher
import com.smartwardrobe.ui.common.uriToBitmap
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

/**
 * 试衣间：拍照提取真人 -> 套用搭配 -> 按部位换装 -> 拖拽缩放对齐。
 * 衣物选择列表直接读 WardrobeRepository（通过 hiltViewModel 注入的共享 ViewModel 由 TryOnViewModel 提供 session）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TryOnScreen(vm: TryOnViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    var showCamera by remember { mutableStateOf(false) }
    val pickLauncher = rememberPickImageLauncher { bitmap -> vm.extractPortrait(bitmap) }

    LaunchedEffect(Unit) { vm.events.collectLatest { /* Toast */ } }

    Scaffold(topBar = { TopAppBar(title = { Text("虚拟试衣间") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showCamera = true }, modifier = Modifier.weight(1f)) { Text("拍照录入真人") }
                OutlinedButton(
                    onClick = { pickLauncher.launch(imageOnlyRequest()) },
                    modifier = Modifier.weight(1f)
                ) { Text("相册选真人") }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                when (val state = uiState) {
                    is TryOnUiState.Idle -> Text("先拍照录入真人形象，\n再套用搭配或自行换装。")
                    is TryOnUiState.Extracting -> CircularProgressIndicator()
                    is TryOnUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                    is TryOnUiState.Ready -> {
                        val preview = state.preview
                        if (preview != null) {
                            Image(
                                bitmap = preview.asImageBitmap(),
                                contentDescription = "试衣效果",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("人像已录入，请换装后查看效果")
                        }
                    }
                }
            }

            if (uiState is TryOnUiState.Ready) {
                val session = (uiState as TryOnUiState.Ready).session
                Text("当前搭配：${session.outfitTitle}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(onClick = { vm.clear() }, modifier = Modifier.weight(1f)) { Text("清空") }
                }
                SlotChanger(vm = vm, sessionLayers = session.layers)
            }
        }
    }

    if (showCamera) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCamera = false },
            confirmButton = {},
            title = { Text("拍照（点击画面拍摄真人）") },
            text = {
                Box(Modifier.fillMaxWidth().height(340.dp)) {
                    CameraXPreview(
                        onPhotoTaken = { file ->
                            showCamera = false
                            vm.extractPortrait(uriToBitmap(context, android.net.Uri.fromFile(file)))
                        },
                        onError = { showCamera = false }
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotChanger(vm: TryOnViewModel, sessionLayers: List<com.smartwardrobe.domain.model.TryOnLayer>) {
    val slots = listOf("TOP_INNER", "TOP_OUTER", "BOTTOM", "ONE_PIECE", "SHOES", "BAG", "ACCESSORY")
    var selectedSlot by remember { mutableStateOf<String?>(null) }

    Text("点击部位换装：", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp))
    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(slots) { slot ->
            val hasItem = sessionLayers.any { it.slot == slot }
            FilterChip(
                selected = hasItem,
                onClick = { selectedSlot = slot },
                label = { Text(slot) }
            )
        }
    }

    selectedSlot?.let { slot ->
        WardrobePickerSheet(
            slot = slot,
            onPick = { itemId -> vm.changeSlot(slot, itemId) },
            onRemove = { vm.changeSlot(slot, null) },
            onDismiss = { selectedSlot = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WardrobePickerSheet(
    slot: String,
    onPick: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val repo = hiltViewModel<WardrobePickerVm>()
    val items by repo.items.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val neededCategory = when (slot) {
        "TOP_INNER" -> Category.TOP_INNER
        "TOP_OUTER" -> Category.TOP_OUTER
        "BOTTOM" -> Category.BOTTOM
        "ONE_PIECE" -> Category.ONE_PIECE
        "SHOES" -> Category.SHOES
        "BAG" -> Category.BAG
        "ACCESSORY" -> Category.ACCESSORY
        else -> null
    }
    val candidates = items.filter { neededCategory == null || it.category == neededCategory }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text("选择要穿的 ${slot}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(modifier = Modifier.clickable { onRemove(); onDismiss() }) {
                    Text("移除", modifier = Modifier.padding(16.dp))
                }
            }
            items(candidates, key = { it.id }) { item ->
                Card(modifier = Modifier.clickable { onPick(item.id); onDismiss() }) {
                    Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(model = item.imagePath, contentDescription = item.name, modifier = Modifier.width(80.dp).height(80.dp))
                        Text(item.name, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** 独立 ViewModel：只读衣橱全量列表供换装选择。 */
@HiltViewModel
class WardrobePickerVm @Inject constructor(repo: WardrobeRepository) : ViewModel() {
    val items = repo.observeClothes(com.smartwardrobe.domain.model.ClothingFilter())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
