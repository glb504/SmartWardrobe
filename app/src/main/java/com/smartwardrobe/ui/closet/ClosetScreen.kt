package com.smartwardrobe.ui.closet

import android.graphics.Bitmap
import com.smartwardrobe.ui.common.imageOnlyRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.smartwardrobe.data.local.entity.Category
import com.smartwardrobe.data.local.entity.ClothingItemEntity
import com.smartwardrobe.data.local.entity.Season
import com.smartwardrobe.domain.usecase.AddClothingState
import com.smartwardrobe.ui.common.CameraXPreview
import com.smartwardrobe.ui.common.createImageFile
import com.smartwardrobe.ui.common.rememberPickImageLauncher
import com.smartwardrobe.ui.common.uriToBitmap
import kotlinx.coroutines.flow.collectLatest

private val COLOR_SWATCHES = listOf(
    "黑色" to "#000000", "白色" to "#FFFFFF", "灰色" to "#9E9E9E",
    "红色" to "#F44336", "蓝色" to "#2196F3", "绿色" to "#4CAF50",
    "黄色" to "#FFEB3B", "棕色" to "#795548", "米色" to "#F5F5DC"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen(vm: ClosetViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val clothes by vm.clothes.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val pipelineState by vm.pipeline.state.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<ClothingItemEntity?>(null) }

    val pickLauncher = rememberPickImageLauncher { bitmap -> vm.startAddFromPhoto(bitmap) }

    LaunchedEffect(Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is ClosetEvent.Saved -> showAddDialog = false
                is ClosetEvent.Message -> { /* 可用 Snackbar 展示，此处省略 */ }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("我的衣橱") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                vm.pipeline.reset()
                showAddDialog = true
            }) { Icon(Icons.Default.Add, "添加衣物") }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // 搜索框
            OutlinedTextField(
                value = filter.keyword,
                onValueChange = { vm.setKeyword(it) },
                label = { Text("搜索名称 / 子品类 / 材质") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                singleLine = true
            )
            // 品类筛选
            FilterRow(label = "品类") {
                FilterChip(selected = filter.category == null, onClick = { vm.setCategory(null) }, label = { Text("全部") })
                Category.entries.forEach { c ->
                    FilterChip(selected = filter.category == c, onClick = { vm.setCategory(c) }, label = { Text(c.name) })
                }
            }
            // 季节筛选
            FilterRow(label = "季节") {
                Season.entries.forEach { s ->
                    FilterChip(selected = s in filter.seasons, onClick = { vm.toggleSeason(s) }, label = { Text(s.name) })
                }
            }
            // 颜色色卡
            FilterRow(label = "颜色") {
                COLOR_SWATCHES.forEach { (name, hex) ->
                    FilterChip(
                        selected = filter.colorFamily == name,
                        onClick = { vm.setColorFamily(if (filter.colorFamily == name) null else name) },
                        label = { Text(name) },
                        leadingIcon = {
                            androidx.compose.foundation.Canvas(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp))) {
                                drawRect(Color(android.graphics.Color.parseColor(hex)))
                            }
                        }
                    )
                }
            }
            // 网格
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(clothes, key = { it.id }) { item ->
                    ClothingCard(item, onClick = { selected = item })
                }
            }
        }
    }

    if (showCamera) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCamera = false },
            confirmButton = {},
            title = { Text("拍照（点击画面拍摄）") },
            text = {
                Box(Modifier.fillMaxWidth().height(320.dp)) {
                    CameraXPreview(
                        onPhotoTaken = { file ->
                            showCamera = false
                            vm.startAddFromPhoto(uriToBitmap(context, android.net.Uri.fromFile(file)))
                        },
                        onError = { showCamera = false }
                    )
                }
            }
        )
    }

    if (showAddDialog) {
        AddClothingDialog(
            state = pipelineState,
            onPickPhoto = { pickLauncher.launch(imageOnlyRequest()) },
            onTakePhoto = { showCamera = true },
            onConfirm = { name, sub, cat, colorName, colorHex, seasons, styles, material, warmth ->
                vm.confirmAdd(name, sub, cat, colorName, colorHex, seasons, styles, material, warmth)
            },
            onCancel = { vm.cancelAdd(); showAddDialog = false }
        )
    }

    selected?.let { item ->
        ClothingDetailSheet(
            item = item,
            onDismiss = { selected = null },
            onMarkWorn = { vm.markWornToday(item) },
            onDelete = { vm.deleteClothing(item); selected = null },
            onSave = { updated -> vm.updateClothing(updated) }
        )
    }
}

@Composable
private fun FilterRow(label: String, content: @Composable () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 12.dp, top = 4.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) { item { content() } }
    }
}

@Composable
private fun ClothingCard(item: ClothingItemEntity, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Box {
            AsyncImage(
                model = item.imagePath,
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(120.dp).padding(4.dp)
            )
            Badge(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                Text("${item.wearCount}次")
            }
        }
        Text(
            item.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClothingDetailSheet(
    item: ClothingItemEntity,
    onDismiss: () -> Unit,
    onMarkWorn: () -> Unit,
    onDelete: () -> Unit,
    onSave: (ClothingItemEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(item.name) }
    var warmth by remember { mutableStateOf(item.warmthLevel) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            AsyncImage(model = item.imagePath, contentDescription = item.name, modifier = Modifier.fillMaxWidth().height(220.dp))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
            Text("品类：${item.category} ｜ 主色：${item.primaryColor} ｜ 保暖度：$warmth", style = MaterialTheme.typography.bodyMedium)
            Text("穿着 ${item.wearCount} 次 ｜ 风格：${item.styles.joinToString("、")}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Row {
                Button(onClick = onMarkWorn, modifier = Modifier.weight(1f)) { Text("今日穿了它") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onSave(item.copy(name = name, warmthLevel = warmth)) }, modifier = Modifier.weight(1f)) { Text("保存修改") }
            }
            TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
        }
    }
}
