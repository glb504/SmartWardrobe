package com.smartwardrobe.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartwardrobe.data.local.entity.Category
import com.smartwardrobe.data.local.entity.ClothingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClothingDao {

    @Query("SELECT * FROM clothing_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ClothingItemEntity>>

    @Query("SELECT * FROM clothing_items WHERE id = :id")
    suspend fun getById(id: String): ClothingItemEntity?

    @Query("SELECT * FROM clothing_items WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ClothingItemEntity>

    @Query("SELECT * FROM clothing_items WHERE category = :category ORDER BY createdAt DESC")
    fun observeByCategory(category: Category): Flow<List<ClothingItemEntity>>

    @Query("SELECT * FROM clothing_items WHERE name LIKE '%' || :keyword || '%' OR subCategory LIKE '%' || :keyword || '%' OR material LIKE '%' || :keyword || '%'")
    fun search(keyword: String): Flow<List<ClothingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClothingItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ClothingItemEntity>)

    @Update
    suspend fun update(item: ClothingItemEntity)

    @Delete
    suspend fun delete(item: ClothingItemEntity)

    @Query("DELETE FROM clothing_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE clothing_items SET wearCount = wearCount + 1, lastWornDate = :wornAt WHERE id = :id")
    suspend fun incrementWearCount(id: String, wornAt: Long)

    @Query("SELECT COUNT(*) FROM clothing_items")
    fun observeCount(): Flow<Int>
}
