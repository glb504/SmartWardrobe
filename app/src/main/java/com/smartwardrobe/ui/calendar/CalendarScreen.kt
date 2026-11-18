package com.smartwardrobe.ui.calendar

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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: CalendarViewModel = hiltViewModel()) {
    val records by vm.records.collectAsStateWithLifecycle()
    val ranking by vm.ranking.collectAsStateWithLifecycle()
    val max = ranking.firstOrNull()?.second?.coerceAtLeast(1) ?: 1

    Scaffold(topBar = { TopAppBar(title = { Text("穿搭日历与统计") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("最近 30 天", style = MaterialTheme.typography.titleMedium)
                        Text("穿着记录 ${records.size} 条；不同衣物 ${ranking.count { it.second > 0 }} 件")
                    }
                }
            }
            item {
                Text("穿着排行", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            items(ranking.filter { it.second > 0 }) { (item, count) ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Row(Modifier.padding(12.dp)) {
                        AsyncImage(model = item.imagePath, contentDescription = item.name, modifier = Modifier.height(56.dp))
                        Spacer(Modifier.padding(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.bodyMedium)
                            Text("穿着 $count 次", style = MaterialTheme.typography.labelSmall)
                            LinearProgressIndicator(progress = { count.toFloat() / max }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}
