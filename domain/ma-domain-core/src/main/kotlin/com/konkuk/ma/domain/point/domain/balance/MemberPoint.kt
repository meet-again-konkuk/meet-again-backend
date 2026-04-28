package com.konkuk.ma.domain.point.domain.balance

import com.konkuk.ma.domain.common.domain.Email

class MemberPoint(
    val id: Long?,
    val ownerEmail: Email,
    val balance: PointQuantity,
) {
    fun charge(quantity: PointQuantity): MemberPoint {
        return MemberPoint(
            id = id,
            ownerEmail = ownerEmail,
            balance = balance + quantity,
        )
    }

    fun spend(quantity: PointQuantity): MemberPoint {
        return MemberPoint(
            id = id,
            ownerEmail = ownerEmail,
            balance = balance - quantity,
        )
    }

    fun isPersisted(): Boolean = id != null

    companion object {
        fun initial(ownerEmail: Email): MemberPoint {
            return MemberPoint(
                id = null,
                ownerEmail = ownerEmail,
                balance = PointQuantity.ZERO,
            )
        }
    }
}
