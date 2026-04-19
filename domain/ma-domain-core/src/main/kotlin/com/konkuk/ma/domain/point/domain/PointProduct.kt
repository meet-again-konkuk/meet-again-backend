package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.common.domain.Money

class PointProduct(
    val pointProductId: Long,
    val name: String,
    val quantity: Int,
    val price: Money,
    val displayOrder: Int,
    val discountPolicyId: Long? = null,
)
