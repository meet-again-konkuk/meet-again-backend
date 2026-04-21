package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import com.konkuk.ma.domain.point.domain.transaction.NewPointTransaction
import com.konkuk.ma.domain.point.domain.transaction.PointTransaction
import com.konkuk.ma.domain.point.domain.transaction.PointTransactionType
import com.konkuk.ma.domain.point.entity.table.PointTransactionTable
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.ResultRow

class PointTransactionEntity(
    val id: Long?,
    val ownerEmail: String,
    val pointProductId: Long?,
    val transactionType: String,
    val quantity: Int,
    val paidAmount: Int,
    val paymentMethod: String?,
    val idempotencyKey: String,
    val approvalNumber: String?,
    val createdDate: LocalDateTime?,
) {
    fun toDomain(): PointTransaction {
        return PointTransaction(
            id = id!!,
            ownerEmail = Email(ownerEmail),
            pointProductId = pointProductId,
            transactionType = PointTransactionType.valueOf(transactionType),
            quantity = PointQuantity(quantity),
            paidAmount = Money.wons(paidAmount),
            paymentMethod = paymentMethod?.let { PaymentMethod.valueOf(it) },
            idempotencyKey = idempotencyKey,
            approvalNumber = approvalNumber,
            createdDate = createdDate!!,
        )
    }

    companion object {
        fun from(row: ResultRow): PointTransactionEntity {
            return PointTransactionEntity(
                id = row[PointTransactionTable.id].value,
                ownerEmail = row[PointTransactionTable.ownerEmail],
                pointProductId = row[PointTransactionTable.pointProductId],
                transactionType = row[PointTransactionTable.transactionType],
                quantity = row[PointTransactionTable.quantity],
                paidAmount = row[PointTransactionTable.paidAmount],
                paymentMethod = row[PointTransactionTable.paymentMethod],
                idempotencyKey = row[PointTransactionTable.idempotencyKey],
                approvalNumber = row[PointTransactionTable.approvalNumber],
                createdDate = row[PointTransactionTable.createdDate],
            )
        }

        fun from(newTransaction: NewPointTransaction): PointTransactionEntity {
            return PointTransactionEntity(
                id = null,
                ownerEmail = newTransaction.ownerEmail.value,
                pointProductId = newTransaction.pointProductId,
                transactionType = newTransaction.transactionType.name,
                quantity = newTransaction.quantity.toInt(),
                paidAmount = newTransaction.paidAmount.toInt(),
                paymentMethod = newTransaction.paymentMethod?.name,
                idempotencyKey = newTransaction.idempotencyKey,
                approvalNumber = newTransaction.approvalNumber,
                createdDate = null,
            )
        }
    }
}
