package com.konkuk.ma.domain.point.domain.balance

class MemberPoint(
    val id: Long?,
    val ownerId: Long,
    val balance: PointQuantity,
) {
    fun charge(quantity: PointQuantity): MemberPoint {
        return MemberPoint(
            id = id,
            ownerId = ownerId,
            balance = balance + quantity,
        )
    }

    fun spend(quantity: PointQuantity): MemberPoint {
        return MemberPoint(
            id = id,
            ownerId = ownerId,
            balance = balance - quantity,
        )
    }

    fun isPersisted(): Boolean = id != null

    companion object {
        fun initial(ownerId: Long): MemberPoint {
            return MemberPoint(
                id = null,
                ownerId = ownerId,
                balance = PointQuantity.ZERO,
            )
        }
    }
}
