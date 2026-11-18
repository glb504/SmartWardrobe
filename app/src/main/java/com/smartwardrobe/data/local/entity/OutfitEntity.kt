package com.smartwardrobe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 搭配方案表。
 *
 * 在给定 schema 基础上增加 [stylingTips] 字段，用于承载 AI 返回的
 * stylingTips（穿搭小贴士），避免结构化信息在落库时丢失。
 */
@Entity(tableName = "outfits")
data class OutfitEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val title: String,

    val clothingItemIds: List<String>,

    val occasion: String,

    val targetTempRange: String,

    val aiReasoning: String,

    val stylingTips: String = "",

    val isFavorite: Boolean = false,

    val createdAt: Long = System.currentTimeMillis()
)
