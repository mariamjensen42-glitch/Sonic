package com.cycling.data.di

import com.cycling.data.api.DeepSeekApiService
import com.cycling.data.api.DeepSeekApiServiceImpl
import com.cycling.data.repository.AiRepositoryImpl
import com.cycling.domain.repository.AiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindDeepSeekApiService(
        impl: DeepSeekApiServiceImpl
    ): DeepSeekApiService

}
