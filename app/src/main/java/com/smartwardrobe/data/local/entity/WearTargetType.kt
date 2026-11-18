package com.smartwardrobe.data.local.entity

import kotlinx.serialization.Serializable

/** 穿着记录的目标类型：单件衣物或整套搭配。 */
@Serializable
enum class WearTargetType {
    ITEM, OUTFIT
}
