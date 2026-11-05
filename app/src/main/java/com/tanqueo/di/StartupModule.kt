package com.tanqueo.di

import com.tanqueo.StartupTask
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
abstract class StartupModule {
    @Multibinds
    abstract fun startupTasks(): Set<StartupTask>
}
