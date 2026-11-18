package com.smartwardrobe.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartwardrobe.data.local.converter.Converters
import com.smartwardrobe.data.local.dao.ClothingDao
import com.smartwardrobe.data.local.dao.OutfitDao
import com.smartwardrobe.data.local.dao.WearRecordDao
import com.smartwardrobe.data.local.entity.ClothingItemEntity
import com.smartwardrobe.data.local.entity.OutfitEntity
import com.smartwardrobe.data.local.entity.WearRecordEntity

@Database(
    entities = [
        ClothingItemEntity::class,
        OutfitEntity::class,
        WearRecordEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WardrobeDatabase : RoomDatabase() {
    abstract fun clothingDao(): ClothingDao
    abstract fun outfitDao(): OutfitDao
    abstract fun wearRecordDao(): WearRecordDao

    companion object {
        const val DATABASE_NAME = "wardrobe.db"
    }
}
