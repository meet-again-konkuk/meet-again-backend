package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.point.entity.PointTransactionEntity
import com.konkuk.ma.domain.point.entity.table.PointTransactionTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class PointTransactionDao {
    fun save(entity: PointTransactionEntity): Long {
        return PointTransactionTable.insertAndGetId {
            it[ownerEmail] = entity.ownerEmail
            it[pointProductId] = entity.pointProductId
            it[transactionType] = entity.transactionType
            it[quantity] = entity.quantity
            it[paidAmount] = entity.paidAmount
            it[paymentMethod] = entity.paymentMethod
            it[idempotencyKey] = entity.idempotencyKey
            it[approvalNumber] = entity.approvalNumber
            it[createdBy] = entity.ownerEmail
            it[lastModifiedBy] = entity.ownerEmail
        }.value
    }

    fun findOneOrNull(idempotencyKey: String): PointTransactionEntity? {
        return PointTransactionTable
            .selectAll()
            .where {
                (PointTransactionTable.idempotencyKey eq idempotencyKey) and
                    (PointTransactionTable.deleted eq false)
            }
            .limit(1)
            .firstOrNull()
            ?.let { PointTransactionEntity.from(it) }
    }
}
