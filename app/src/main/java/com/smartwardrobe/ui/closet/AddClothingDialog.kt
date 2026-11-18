package com.smartwardrobe.ui.closet

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.smartwardrobe.data.local.entity.Category
import com.smartwardrobe.data.local.entity.Season
import com.smartwardrobe.domain.model.ClothingAnalysis
import com.smartwardrobe.domain.usecase.AddClothingState

/** 录入弹窗：抠图预览 -> AI 自动填充表单 -> 用户微调 -> 保存。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClothingDialog(
    state: AddClothingState,
    onPickPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onConfirm: (name: String, sub: String, cat: Category, colorName: String, colorHex: String,
        seasons: List<Season>, styles: List<String>, material: String, warmth: Int) -> Unit,
    onCancel: () -> Unit
) {
    // 表单状态，随 ReadyToConfirm 初始化一次
    var initialized by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var sub by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(Category.TOP_INNER) }
    var colorName by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf("#CCCCCC") }
    var seasons by remember { mutableStateOf<Set<Season>>(emptySet()) }
    var stylesText by remember { mutableStateOf("") }
    var material by remember { mutableStateOf("") }
    var warmth by remember { mutableStateOf(3) }

    val ready = state as? AddClothingState.ReadyToConfirm
    LaunchedEffect(ready) {
        if (ready != null && !initialized) {
            val a: ClothingAnalysis = ready.analysis
            name = a.name
            sub = a.subCategory
            cat = a.category
            colorName = a.primaryColor
            colorHex = a.primaryColorHex
            seasons = a.seasons.toSet()
            stylesText = a.styles.joinToString("、")
            material = a.material
            warmth = a.warmthLevel
            initialized = true
        }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("添加衣物") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when (state) {
                    is AddClothingState.Idle -> SourcePicker(onPickPhoto, onTakePhoto)
                    is AddClothingState.RemovingBackground -> ProcessingView("正在端侧抠图…")
                    is AddClothingState.Analyzing -> ProcessingView("AI 正在识别属性…")
                    is AddClothingState.Saving -> ProcessingView("正在保存…")
                    is AddClothingState.Error -> Text("出错了：${state.message}（${state.stage}）", color = MaterialTheme.colorScheme.error)
                    is AddClothingState.Saved -> Text("已保存 ✓")
                    is AddClothingState.ReadyToConfirm -> {
                        // 抠图预览
                        AsyncImage(
                            model = ready!!.imagePath,
                            contentDescription = "抠图预览",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("AI 已自动识别，可微调：", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = sub, onValueChange = { sub = it }, label = { Text("子品类（如 牛仔裤）") }, modifier = Modifier.fillMaxWidth())
                        CategoryDropdown(cat) { cat = it }
                        OutlinedTextField(value = colorName, onValueChange = { colorName = it }, label = { Text("主色调") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = colorHex, onValueChange = { colorHex = it }, label = { Text("颜色 Hex") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = stylesText, onValueChange = { stylesText = it }, label = { Text("风格（顿号分隔）") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = material, onValueChange = { material = it }, label = { Text("材质") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                        Text("适用季节：", style = MaterialTheme.typography.labelMedium)
                        Row {
                            Season.entries.forEach { s ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = s in seasons, onCheckedChange = {
                                        seasons = seasons.toMutableSet().apply { if (!add(s)) remove(s) }
                                    })
                                    Text(s.name)
                                }
                            }
                        }
                        Text("保暖度：$warmth（1 极薄 ~ 5 严寒）", style = MaterialTheme.typography.labelMedium)
                        Row {
                            (1..5).forEach { level ->
                                Button(onClick = { warmth = level }, modifier = Modifier.weight(1f)) {
                                    Text("$level")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (ready != null) {
                Button(onClick = {
                    onConfirm(
                        name, sub, cat, colorName, colorHex,
                        seasons.toList(),
                        stylesText.split("、", ",").map { it.trim() }.filter { it.isNotEmpty() },
                        material, warmth
                    )
                }) { Text("保存入库") }
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("取消") } }
    )
}

@Composable
private fun SourcePicker(onPick: () -> Unit, onTake: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("拍照或从相册选择一件衣物，\n将自动抠除背景并 AI 识别属性。")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onTake, modifier = Modifier.fillMaxWidth()) { Text("打开相机拍照") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) { Text("从相册选择") }
    }
}

@Composable
private fun ProcessingView(message: String) {
    // 不指定 progress 的 CircularProgressIndicator 自带无限旋转动画
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(message)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(current: Category, onChange: (Category) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("品类") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Category.entries.forEach { c ->
                DropdownMenuItem(text = { Text(c.name) }, onClick = { onChange(c); expanded = false })
            }
        }
    }
}
