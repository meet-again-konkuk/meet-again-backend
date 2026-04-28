package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.point.domain.balance.MemberPoint
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.entity.table.MemberPointTable
import org.jetbrains.exposed.sql.ResultRow

class MemberPointEntity(
    val id: Long?,
    val ownerEmail: String,
    val balance: Int,
) {
    fun toDomain(): MemberPoint {
        return MemberPoint(
            id = id,
            ownerEmail = Email(ownerEmail),
            balance = PointQuantity(balance),
        )
    }

    companion object {
        fun from(row: ResultRow): MemberPointEntity {
            return MemberPointEntity(
                id = row[MemberPointTable.id].value,
                ownerEmail = row[MemberPointTable.ownerEmail],
                balance = row[MemberPointTable.balance],
            )
        }

        fun from(memberPoint: MemberPoint): MemberPointEntity {
            return MemberPointEntity(
                id = memberPoint.id,
                ownerEmail = memberPoint.ownerEmail.value,
                balance = memberPoint.balance.toInt(),
            )
        }
    }
}
