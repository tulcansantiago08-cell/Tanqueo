package com.tanqueo.data.local

import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.repository.FillUpRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomFillUpRepository @Inject constructor(private val dao: FillUpDao) : FillUpRepository {
    override fun observeAll(): Flow<List<FillUp>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observe(id: Long): Flow<FillUp?> = dao.observe(id).map { it?.toDomain() }

    override suspend fun add(fillUp: FillUp): Long = dao.insert(fillUp.toEntity())

    override suspend fun delete(id: Long) = dao.delete(id)
}
