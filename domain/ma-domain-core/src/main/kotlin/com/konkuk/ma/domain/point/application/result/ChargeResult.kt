package com.konkuk.ma.domain.point.application.result

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.balance.PointQuantity

class ChargeResult(
    val pointTransactionId: Long,
    val balance: PointQuantity,
    val chargedQuantity: PointQuantity,
    val paidAmount: Money,
    val approvalNumber: String,
)
