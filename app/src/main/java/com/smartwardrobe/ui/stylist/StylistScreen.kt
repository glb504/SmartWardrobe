package com.smartwardrobe.ui.stylist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartwardrobe.data.local.entity.Season
import com.smartwardrobe.domain.usecase.ResolvedOutfit
import kotlinx.coroutines.flow.collectLatest

private val OCCASIONS = listOf("通勤", "约会", "运动", "旅行", "聚会", "日常")
private val WEATHERS = listOf("晴", "多云", "阴", "雨", "雪", "大风")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylistScreen(
    onTryOn: () -> Unit,
    vm: StylistViewModel = hiltViewModel()
) {
    val context by vm.context.collectAsStateWithLifecycle()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()

    var temp by remember { mutableFloatStateOf(context.temperatureC.toFloat()) }
    LaunchedEffect(Unit) {
        vm.events.collectLatest { /* Toast 可在此展示 */ }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("AI 穿搭造型师") }) }) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("今天是什么场合？", style = MaterialTheme.typography.titleSmall)
                    DropdownField("场合", context.occasion, OCCASIONS) { vm.setOccasion(it) }
                    Spacer(Modifier.height(6.dp))
                    DropdownField("天气", context.weather, WEATHERS) { vm.setWeather(it) }
                    Spacer(Modifier.height(6.dp))
                    Text("温度：${temp.toInt()}°C", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = temp,
                        onValueChange = { temp = it; vm.setTemperature(it.toInt()) },
                        valueRange = -10f..40f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = context.moodOrRequest,
                        onValueChange = { vm.setMood(it) },
                        label = { Text("心情 / 特殊要求（如：想穿新买的牛仔裤）") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { vm.generate() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is StylistUiState.Loading
                    ) {
                        Text(if (uiState is StylistUiState.Loading) "生成中…" else "生成穿搭方案")
                    }
                }
            }

            when (val state = uiState) {
                is StylistUiState.Loading -> item {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text(state.step)
                    }
                }
                is StylistUiState.Error -> item {
                    Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
                is StylistUiState.Success -> {
                    items(state.outfits) { resolved ->
                        OutfitCollageCard(
                            resolved = resolved,
                            isFavorite = favorites.any { it.id == resolved.entity.id },
                            onToggleFavorite = { vm.toggleFavorite(resolved.entity) },
                            onTryOn = { vm.prepareTryOn(resolved.entity); onTryOn() }
                        )
                    }
                    item {
                        Button(onClick = { vm.regenerate() }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                            Text("换一批")
                        }
                    }
                }
                is StylistUiState.Idle -> item {
                    if (history.isNotEmpty()) {
                        Text("历史搭配（${history.size}）", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(label: String, current: String, options: List<String>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onChange(opt); expanded = false })
            }
        }
    }
}
