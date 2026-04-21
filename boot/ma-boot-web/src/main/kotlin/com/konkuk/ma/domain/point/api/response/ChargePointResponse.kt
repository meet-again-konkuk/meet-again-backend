package com.konkuk.ma.domain.point.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.point.application.result.ChargeResult
import com.konkuk.ma.support.id.EncryptId

class ChargePointResponse(
    @EncryptId(ObfuscationType.POINT_TRANSACTION)
    val pointTransactionId: Long,
    val balance: Int,
    val chargedQuantity: Int,
    val paidAmount: Int,
    val approvalNumber: String,
) {
    companion object {
        fun from(result: ChargeResult): ChargePointResponse {
            return ChargePointResponse(
                pointTransactionId = result.pointTransactionId,
                balance = result.balance.toInt(),
                chargedQuantity = result.chargedQuantity.toInt(),
                paidAmount = result.paidAmount.toInt(),
                approvalNumber = result.approvalNumber,
            )
        }
    }
}
