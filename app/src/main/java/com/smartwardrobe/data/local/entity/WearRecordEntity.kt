package com.smartwardrobe.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 穿着记录表（穿搭日历/统计的数据来源）。
 *
 * 用户 schema 只定义了衣物表与搭配表；为满足「穿搭日历记录」存储需求
 * 以及「历史搭配」「今日穿了它」等交互，补充本表。
 */
@Entity(
    tableName = "wear_records",
    indices = [Index(value = ["dateKey"]), Index(value = ["targetId"])]
)
data class WearRecordEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val targetId: String,

    val targetType: WearTargetType,

    /** 本地日期键，格式 yyyy-MM-dd，按天查询日历时使用。 */
    val dateKey: String,

    val wornAt: Long = System.currentTimeMillis(),

    val note: String? = null
)
