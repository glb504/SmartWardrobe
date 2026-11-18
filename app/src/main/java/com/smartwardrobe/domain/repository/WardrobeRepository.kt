package com.smartwardrobe.domain.repository

import com.smartwardrobe.data.local.entity.ClothingItemEntity
import com.smartwardrobe.data.local.entity.OutfitEntity
import com.smartwardrobe.data.local.entity.WearRecordEntity
import com.smartwardrobe.domain.model.ClothingFilter
import kotlinx.coroutines.flow.Flow

/**
 * 衣橱仓库契约。Domain 层只依赖此接口，不感知 Room / DataStore 的具体实现。
 */
interface WardrobeRepository {

    // ---------- 衣物 ----------
    fun observeClothes(filter: ClothingFilter): Flow<List<ClothingItemEntity>>
    suspend fun getClothing(id: String): ClothingItemEntity?
    suspend fun getClothesByIds(ids: List<String>): List<ClothingItemEntity>
    suspend fun addClothing(item: ClothingItemEntity)
    suspend fun updateClothing(item: ClothingItemEntity)
    suspend fun deleteClothing(id: String)
    suspend fun markWorn(id: String, wornAt: Long, dateKey: String)
    fun observeClothingCount(): Flow<Int>

    // ---------- 搭配 ----------
    fun observeOutfits(): Flow<List<OutfitEntity>>
    fun observeFavoriteOutfits(): Flow<List<OutfitEntity>>
    fun observeOutfitsContainingItem(itemId: String): Flow<List<OutfitEntity>>
    suspend fun getOutfit(id: String): OutfitEntity?
    suspend fun saveOutfits(outfits: List<OutfitEntity>)
    suspend fun setOutfitFavorite(id: String, isFavorite: Boolean)
    suspend fun deleteOutfit(id: String)

    // ---------- 穿着记录 ----------
    fun observeWearRecordsByDate(dateKey: String): Flow<List<WearRecordEntity>>
    fun observeWearRecordsByRange(startDate: String, endDate: String): Flow<List<WearRecordEntity>>
    suspend fun addWearRecord(record: WearRecordEntity)
}
