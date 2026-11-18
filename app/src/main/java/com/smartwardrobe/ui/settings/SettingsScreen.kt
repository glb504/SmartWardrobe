package com.smartwardrobe.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val apiKey by vm.apiKey.collectAsStateWithLifecycle()
    val bodyType by vm.bodyType.collectAsStateWithLifecycle()
    val skinTone by vm.skinTone.collectAsStateWithLifecycle()
    val darkMode by vm.darkMode.collectAsStateWithLifecycle()

    var apiInput by remember(apiKey) { mutableStateOf(apiKey) }
    var bodyInput by remember(bodyType) { mutableStateOf(bodyType) }
    var skinInput by remember(skinTone) { mutableStateOf(skinTone) }

    Scaffold(topBar = { TopAppBar(title = { Text("设置") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            Text("AI 配置", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = apiInput,
                onValueChange = { apiInput = it; vm.setApiKey(it) },
                label = { Text("DeepSeek API Key") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("Key 仅保存在本机 DataStore，用于视觉打标与穿搭生成。", style = MaterialTheme.typography.labelSmall)

            Spacer(Modifier.height(20.dp))
            Text("个人特征（影响 AI 推荐）", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = bodyInput,
                onValueChange = { bodyInput = it; vm.setBodyType(it) },
                label = { Text("体型（如：高挑 / 娇小 / 标准）") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = skinInput,
                onValueChange = { skinInput = it; vm.setSkinTone(it) },
                label = { Text("肤色（如：冷白皮 / 暖黄皮 / 自然）") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            Text("外观", style = MaterialTheme.typography.titleMedium)
            ThemeDropdown(current = darkMode) { vm.setDarkMode(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDropdown(current: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色")
    val label = options.firstOrNull { it.first == current }?.second ?: "跟随系统"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("主题模式") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onChange(value); expanded = false })
            }
        }
    }
}
