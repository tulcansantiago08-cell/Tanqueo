package com.tanqueo.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.tanqueo.data.ai.GeminiFuelAssistant
import com.tanqueo.data.local.FillUpDao
import com.tanqueo.data.local.RoomFillUpRepository
import com.tanqueo.data.local.TanqueoDatabase
import com.tanqueo.data.settings.DataStoreSettingsRepository
import com.tanqueo.data.stations.AndroidLocationSource
import com.tanqueo.data.stations.FallbackStationDirectory
import com.tanqueo.data.stations.FileStationCache
import com.tanqueo.domain.stations.FindGasStations
import com.tanqueo.domain.stations.LocationSource
import com.tanqueo.domain.stations.StationCache
import com.tanqueo.domain.stations.StationDirectory
import com.tanqueo.domain.repository.FillUpRepository
import com.tanqueo.domain.repository.FuelAssistant
import com.tanqueo.domain.repository.SettingsRepository
import com.tanqueo.domain.repository.SurfaceRefresher
import com.tanqueo.tile.WatchSurfacesRefresher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "ajustes")

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindFillUpRepository(impl: RoomFillUpRepository): FillUpRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindFuelAssistant(impl: GeminiFuelAssistant): FuelAssistant

    @Binds
    abstract fun bindSurfaceRefresher(impl: WatchSurfacesRefresher): SurfaceRefresher

    @Binds
    abstract fun bindLocationSource(impl: AndroidLocationSource): LocationSource

    @Binds
    abstract fun bindStationDirectory(impl: FallbackStationDirectory): StationDirectory

    @Binds
    abstract fun bindStationCache(impl: FileStationCache): StationCache

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): TanqueoDatabase =
            Room.databaseBuilder(context, TanqueoDatabase::class.java, "tanqueo.db")
                .addMigrations(TanqueoDatabase.MIGRATION_1_2)
                .build()

        @Provides
        fun provideFillUpDao(database: TanqueoDatabase): FillUpDao = database.fillUpDao()

        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.settingsDataStore

        @Provides
        fun provideClock(): Clock = Clock.systemDefaultZone()

        @Provides
        fun provideFindGasStations(
            location: LocationSource,
            directory: StationDirectory,
            cache: StationCache,
            clock: Clock,
        ) = FindGasStations(location, directory, cache, clock)
    }
}
