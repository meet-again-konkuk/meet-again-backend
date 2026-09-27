package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.balance.PointQuantity

class PointProduct(
    val pointProductId: Long,
    val name: String,
    val quantity: PointQuantity,
    val price: Money,
    val displayOrder: Int,
    val discountPolicyId: Long? = null,
)
