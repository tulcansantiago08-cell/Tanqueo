package com.tanqueo.data.local

import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.VehicleType
import com.tanqueo.domain.repository.FillUpRepository
import com.tanqueo.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class RoomFillUpRepository @Inject constructor(
    private val dao: FillUpDao,
    private val settings: SettingsRepository,
) : FillUpRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<FillUp>> =
        settings.settings.map { it.vehicle }.distinctUntilChanged().flatMapLatest(::observeAll)

    override fun observeAll(vehicle: VehicleType): Flow<List<FillUp>> =
        dao.observeAll(vehicle.name).map { list -> list.map { it.toDomain() } }

    override fun observe(id: Long): Flow<FillUp?> = dao.observe(id).map { it?.toDomain() }

    override suspend fun add(fillUp: FillUp): Long = dao.insert(fillUp.toEntity())

    override suspend fun delete(id: Long) = dao.delete(id)
}
