package com.smartwardrobe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartwardrobe.data.local.entity.WearRecordEntity
import com.smartwardrobe.data.local.entity.WearTargetType
import kotlinx.coroutines.flow.Flow

@Dao
interface WearRecordDao {

    @Query("SELECT * FROM wear_records ORDER BY wornAt DESC")
    fun observeAll(): Flow<List<WearRecordEntity>>

    @Query("SELECT * FROM wear_records WHERE dateKey = :dateKey ORDER BY wornAt DESC")
    fun observeByDate(dateKey: String): Flow<List<WearRecordEntity>>

    @Query("SELECT * FROM wear_records WHERE dateKey BETWEEN :startDate AND :endDate ORDER BY wornAt DESC")
    fun observeByDateRange(startDate: String, endDate: String): Flow<List<WearRecordEntity>>

    @Query("SELECT * FROM wear_records WHERE targetId = :targetId AND targetType = :targetType ORDER BY wornAt DESC")
    fun observeByTarget(targetId: String, targetType: WearTargetType): Flow<List<WearRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: WearRecordEntity)

    @Query("DELETE FROM wear_records WHERE id = :id")
    suspend fun deleteById(id: String)
}
