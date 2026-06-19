package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.point.entity.PointHistoryEntity
import com.konkuk.ma.domain.point.entity.table.PointHistoryTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class PointHistoryDao {
    fun save(entity: PointHistoryEntity): Long {
        return PointHistoryTable.insertAndGetId {
            it[ownerId] = entity.ownerId
            it[pointProductId] = entity.pointProductId
            it[historyType] = entity.historyType
            it[quantity] = entity.quantity
            it[paidAmount] = entity.paidAmount
            it[paymentMethod] = entity.paymentMethod
            it[idempotencyKey] = entity.idempotencyKey
            it[approvalNumber] = entity.approvalNumber
            it[createdBy] = entity.ownerId.toString()
            it[lastModifiedBy] = entity.ownerId.toString()
        }.value
    }

    fun findOneOrNull(idempotencyKey: String): PointHistoryEntity? {
        return PointHistoryTable
            .selectAll()
            .where {
                (PointHistoryTable.idempotencyKey eq idempotencyKey) and
                    (PointHistoryTable.deleted eq false)
            }
            .limit(1)
            .firstOrNull()
            ?.let { PointHistoryEntity.from(it) }
    }

    fun find(ownerId: Long): List<PointHistoryEntity> {
        return PointHistoryTable
            .activeRows { PointHistoryTable.ownerId eq ownerId }
            .map { PointHistoryEntity.from(it) }
    }
}
