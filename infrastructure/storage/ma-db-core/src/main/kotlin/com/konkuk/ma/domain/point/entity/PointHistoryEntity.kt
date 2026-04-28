package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.history.NewPointHistory
import com.konkuk.ma.domain.point.domain.history.PointHistory
import com.konkuk.ma.domain.point.domain.history.PointHistoryType
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import com.konkuk.ma.domain.point.entity.table.PointHistoryTable
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.ResultRow

class PointHistoryEntity(
    val id: Long?,
    val ownerEmail: String,
    val pointProductId: Long?,
    val historyType: String,
    val quantity: Int,
    val paidAmount: Int,
    val paymentMethod: String?,
    val idempotencyKey: String,
    val approvalNumber: String?,
    val createdDate: LocalDateTime?,
) {
    fun toDomain(): PointHistory {
        return PointHistory(
            id = id!!,
            ownerEmail = Email(ownerEmail),
            pointProductId = pointProductId,
            historyType = PointHistoryType.valueOf(historyType),
            quantity = PointQuantity(quantity),
            paidAmount = Money.wons(paidAmount),
            paymentMethod = paymentMethod?.let { PaymentMethod.valueOf(it) },
            idempotencyKey = idempotencyKey,
            approvalNumber = approvalNumber,
            createdDate = createdDate!!,
        )
    }

    companion object {
        fun from(row: ResultRow): PointHistoryEntity {
            return PointHistoryEntity(
                id = row[PointHistoryTable.id].value,
                ownerEmail = row[PointHistoryTable.ownerEmail],
                pointProductId = row[PointHistoryTable.pointProductId],
                historyType = row[PointHistoryTable.historyType],
                quantity = row[PointHistoryTable.quantity],
                paidAmount = row[PointHistoryTable.paidAmount],
                paymentMethod = row[PointHistoryTable.paymentMethod],
                idempotencyKey = row[PointHistoryTable.idempotencyKey],
                approvalNumber = row[PointHistoryTable.approvalNumber],
                createdDate = row[PointHistoryTable.createdDate],
            )
        }

        fun from(newHistory: NewPointHistory): PointHistoryEntity {
            return PointHistoryEntity(
                id = null,
                ownerEmail = newHistory.ownerEmail.value,
                pointProductId = newHistory.pointProductId,
                historyType = newHistory.historyType.name,
                quantity = newHistory.quantity.toInt(),
                paidAmount = newHistory.paidAmount.toInt(),
                paymentMethod = newHistory.paymentMethod?.name,
                idempotencyKey = newHistory.idempotencyKey,
                approvalNumber = newHistory.approvalNumber,
                createdDate = null,
            )
        }
    }
}
