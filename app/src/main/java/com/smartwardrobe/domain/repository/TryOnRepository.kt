package com.smartwardrobe.domain.repository

import com.smartwardrobe.domain.model.TryOnSession

/** 虚拟试衣间契约。 */
interface TryOnRepository {
    /** 保存用户真人透明形象，返回路径。 */
    suspend fun savePortrait(portraitPath: String): String

    /** 获取当前试衣会话（从内存/持久化恢复）。 */
    suspend fun getSession(): TryOnSession?

    /** 用一组衣物 id 建立/替换试衣会话的图层（一键套用搭配）。 */
    suspend fun buildSessionFromItems(itemIds: List<String>, title: String): TryOnSession

    /** 替换某个槽位的衣物（用户自行换装），传 null 表示移除该图层。 */
    suspend fun replaceSlot(slot: String, itemId: String?): TryOnSession

    /** 更新某图层变换参数（拖拽/缩放/旋转）。 */
    suspend fun updateTransform(slot: String, transform: com.smartwardrobe.domain.model.LayerTransform): TryOnSession

    /** 清空试衣间。 */
    suspend fun clearSession()
}
