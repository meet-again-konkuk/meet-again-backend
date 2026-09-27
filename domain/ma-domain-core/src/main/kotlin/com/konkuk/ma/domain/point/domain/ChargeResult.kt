package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.balance.PointQuantity

class ChargeResult(
    val pointHistoryId: Long,
    val balance: PointQuantity,
    val chargedQuantity: PointQuantity,
    val paidAmount: Money,
    val approvalNumber: String,
)
