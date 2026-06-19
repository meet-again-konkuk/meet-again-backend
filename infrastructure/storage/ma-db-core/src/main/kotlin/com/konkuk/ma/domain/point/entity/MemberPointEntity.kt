package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.point.domain.balance.MemberPoint
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.entity.table.MemberPointTable
import org.jetbrains.exposed.sql.ResultRow

class MemberPointEntity(
    val id: Long?,
    val ownerId: Long,
    val balance: Int,
) {
    fun toDomain(): MemberPoint {
        return MemberPoint(
            id = id,
            ownerId = ownerId,
            balance = PointQuantity(balance),
        )
    }

    companion object {
        fun from(row: ResultRow): MemberPointEntity {
            return MemberPointEntity(
                id = row[MemberPointTable.id].value,
                ownerId = row[MemberPointTable.ownerId],
                balance = row[MemberPointTable.balance],
            )
        }

        fun from(memberPoint: MemberPoint): MemberPointEntity {
            return MemberPointEntity(
                id = memberPoint.id,
                ownerId = memberPoint.ownerId,
                balance = memberPoint.balance.toInt(),
            )
        }
    }
}
