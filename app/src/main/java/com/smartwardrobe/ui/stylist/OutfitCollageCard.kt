package com.smartwardrobe.ui.stylist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.smartwardrobe.data.local.entity.ClothingItemEntity
import com.smartwardrobe.domain.usecase.ResolvedOutfit

/**
 * 杂志风拼贴卡片：按【外套+内搭 / 下装 / 鞋子+配饰】分区排布透明 PNG。
 * 每个单品格子带浅色底，模拟杂志排版。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OutfitCollageCard(
    resolved: ResolvedOutfit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onTryOn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(resolved.entity.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "适用 ${resolved.entity.targetTempRange} ｜ ${resolved.entity.occasion}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 拼贴画布：FlowRow 让透明单品错落排布
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                resolved.items.forEach { item ->
                    GarmentTile(item)
                }
            }

            Text("搭配亮点：${resolved.entity.aiReasoning}", style = MaterialTheme.typography.bodyMedium)
            if (resolved.entity.stylingTips.isNotBlank()) {
                Text("小贴士：${resolved.entity.stylingTips}", style = MaterialTheme.typography.bodySmall)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = isFavorite, onClick = onToggleFavorite, label = {
                    Text(if (isFavorite) "已收藏" else "收藏")
                })
                FilterChip(selected = false, onClick = onTryOn, label = { Text("试穿") })
            }
        }
    }
}

@Composable
private fun GarmentTile(item: ClothingItemEntity) {
    Box(
        modifier = Modifier
            .width(88.dp)
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = item.imagePath,
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(0.9f)
        )
    }
}
