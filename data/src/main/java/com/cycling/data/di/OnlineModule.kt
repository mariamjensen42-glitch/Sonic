package com.cycling.data.di

import com.cycling.data.online.lx.LxPluginManager
import com.cycling.data.online.search.OnlineSearchRepositoryImpl
import com.cycling.domain.online.repository.OnlinePluginRepository
import com.cycling.domain.online.repository.OnlineSearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 在线音源（洛雪式脚本取链）的绑定。
 *
 * [OnlinePluginRepository] 只有 [LxPluginManager] 一个实现：脚本导入/排序/屏蔽/取链都在它身上，
 * 脚本运行时（QuickJS）与 HTTP 桥是它内部依赖的普通 `@Singleton`。
 *
 * [OnlineSearchRepository] 是宿主的搜索层（音源脚本不提供搜索），见 [OnlineSearchRepositoryImpl]。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OnlineModule {

    @Binds
    @Singleton
    abstract fun bindOnlinePluginRepository(impl: LxPluginManager): OnlinePluginRepository

    @Binds
    @Singleton
    abstract fun bindOnlineSearchRepository(
        impl: OnlineSearchRepositoryImpl
    ): OnlineSearchRepository
}
