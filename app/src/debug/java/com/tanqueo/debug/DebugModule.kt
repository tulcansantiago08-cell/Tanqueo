package com.tanqueo.debug

import com.tanqueo.StartupTask
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class DebugModule {
    @Binds
    @IntoSet
    abstract fun bindSeed(seed: SeedSampleData): StartupTask
}
