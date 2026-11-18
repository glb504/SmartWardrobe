package com.smartwardrobe.data.repository

import androidx.room.withTransaction
import com.smartwardrobe.data.local.dao.ClothingDao
import com.smartwardrobe.data.local.dao.OutfitDao
import com.smartwardrobe.data.local.dao.WearRecordDao
import com.smartwardrobe.data.local.database.WardrobeDatabase
import com.smartwardrobe.data.local.entity.ClothingItemEntity
import com.smartwardrobe.data.local.entity.OutfitEntity
import com.smartwardrobe.data.local.entity.WearRecordEntity
import com.smartwardrobe.data.local.entity.WearTargetType
import com.smartwardrobe.domain.model.ClothingFilter
import com.smartwardrobe.domain.repository.WardrobeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WardrobeRepositoryImpl @Inject constructor(
    private val database: WardrobeDatabase,
    private val clothingDao: ClothingDao,
    private val outfitDao: OutfitDao,
    private val wearRecordDao: WearRecordDao
) : WardrobeRepository {

    // ---------- 衣物 ----------

    override fun observeClothes(filter: ClothingFilter): Flow<List<ClothingItemEntity>> =
        clothingDao.observeAll().map { items -> items.filter { matchesFilter(it, filter) } }

    /**
     * 多维筛选：品类、季节、风格、色卡、关键词。
     * 在内存中对 Flow 结果过滤，避免为 List 字段编写复杂的 SQL LIKE 拼接。
     */
    private fun matchesFilter(item: ClothingItemEntity, f: ClothingFilter): Boolean {
        if (f.category != null && item.category != f.category) return false
        if (f.seasons.isNotEmpty() && item.seasons.none { it in f.seasons }) return false
        if (f.styles.isNotEmpty() && item.styles.none { style -> f.styles.any { it == style } }) return false
        if (!f.colorFamily.isNullOrBlank()) {
            val target = f.colorFamily.trim()
            val colorOk = item.colorName.contains(target, ignoreCase = true) ||
                item.primaryColor.contains(target, ignoreCase = true)
            if (!colorOk) return false
        }
        if (f.keyword.isNotBlank()) {
            val k = f.keyword.trim()
            val keywordOk = item.name.contains(k, ignoreCase = true) ||
                item.subCategory.contains(k, ignoreCase = true) ||
                item.material.contains(k, ignoreCase = true)
            if (!keywordOk) return false
        }
        return true
    }

    override suspend fun getClothing(id: String): ClothingItemEntity? = clothingDao.getById(id)

    override suspend fun getClothesByIds(ids: List<String>): List<ClothingItemEntity> =
        if (ids.isEmpty()) emptyList() else clothingDao.getByIds(ids)

    override suspend fun addClothing(item: ClothingItemEntity) = clothingDao.insert(item)

    override suspend fun updateClothing(item: ClothingItemEntity) = clothingDao.update(item)

    override suspend fun deleteClothing(id: String) = clothingDao.deleteById(id)

    /** 「今日穿了它」：在同一事务里更新穿着次数并写入日历记录，保证一致性。 */
    override suspend fun markWorn(id: String, wornAt: Long, dateKey: String) {
        database.withTransaction {
            clothingDao.incrementWearCount(id, wornAt)
            wearRecordDao.insert(
                WearRecordEntity(
                    targetId = id,
                    targetType = WearTargetType.ITEM,
                    dateKey = dateKey,
                    wornAt = wornAt
                )
            )
        }
    }

    override fun observeClothingCount(): Flow<Int> = clothingDao.observeCount()

    // ---------- 搭配 ----------

    override fun observeOutfits(): Flow<List<OutfitEntity>> = outfitDao.observeAll()

    override fun observeFavoriteOutfits(): Flow<List<OutfitEntity>> = outfitDao.observeFavorites()

    override fun observeOutfitsContainingItem(itemId: String): Flow<List<OutfitEntity>> =
        outfitDao.observeContainingItem(itemId)

    override suspend fun getOutfit(id: String): OutfitEntity? = outfitDao.getById(id)

    override suspend fun saveOutfits(outfits: List<OutfitEntity>) = outfitDao.insertAll(outfits)

    override suspend fun setOutfitFavorite(id: String, isFavorite: Boolean) =
        outfitDao.setFavorite(id, isFavorite)

    override suspend fun deleteOutfit(id: String) = outfitDao.deleteById(id)

    // ---------- 穿着记录 ----------

    override fun observeWearRecordsByDate(dateKey: String): Flow<List<WearRecordEntity>> =
        wearRecordDao.observeByDate(dateKey)

    override fun observeWearRecordsByRange(
        startDate: String,
        endDate: String
    ): Flow<List<WearRecordEntity>> = wearRecordDao.observeByDateRange(startDate, endDate)

    override suspend fun addWearRecord(record: WearRecordEntity) = wearRecordDao.insert(record)
}
