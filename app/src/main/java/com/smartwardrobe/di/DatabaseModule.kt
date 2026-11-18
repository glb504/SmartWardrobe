package com.smartwardrobe.di

import android.content.Context
import androidx.room.Room
import com.smartwardrobe.data.local.dao.ClothingDao
import com.smartwardrobe.data.local.dao.OutfitDao
import com.smartwardrobe.data.local.dao.WearRecordDao
import com.smartwardrobe.data.local.database.WardrobeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WardrobeDatabase =
        Room.databaseBuilder(
            context,
            WardrobeDatabase::class.java,
            WardrobeDatabase.DATABASE_NAME
        ).build()

    @Provides
    fun provideClothingDao(database: WardrobeDatabase): ClothingDao = database.clothingDao()

    @Provides
    fun provideOutfitDao(database: WardrobeDatabase): OutfitDao = database.outfitDao()

    @Provides
    fun provideWearRecordDao(database: WardrobeDatabase): WearRecordDao = database.wearRecordDao()
}
