package com.smartwardrobe.di

import com.smartwardrobe.data.repository.AiRepositoryImpl
import com.smartwardrobe.data.repository.TryOnRepositoryImpl
import com.smartwardrobe.data.repository.WardrobeRepositoryImpl
import com.smartwardrobe.domain.repository.AiRepository
import com.smartwardrobe.domain.repository.TryOnRepository
import com.smartwardrobe.domain.repository.WardrobeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWardrobeRepository(impl: WardrobeRepositoryImpl): WardrobeRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    @Binds
    @Singleton
    abstract fun bindTryOnRepository(impl: TryOnRepositoryImpl): TryOnRepository
}
