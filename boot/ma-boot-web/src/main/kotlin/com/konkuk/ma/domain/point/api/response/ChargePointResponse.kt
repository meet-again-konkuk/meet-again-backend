package com.konkuk.ma.domain.point.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.point.domain.ChargeResult
import com.konkuk.ma.support.id.EncryptId

class ChargePointResponse(
    @EncryptId(ObfuscationType.POINT_HISTORY)
    val pointHistoryId: Long,
    val balance: Int,
    val chargedQuantity: Int,
    val paidAmount: Int,
    val approvalNumber: String,
) {
    companion object {
        fun from(result: ChargeResult): ChargePointResponse {
            return ChargePointResponse(
                pointHistoryId = result.pointHistoryId,
                balance = result.balance.toInt(),
                chargedQuantity = result.chargedQuantity.toInt(),
                paidAmount = result.paidAmount.toInt(),
                approvalNumber = result.approvalNumber,
            )
        }
    }
}
