package com.konkuk.ma.domain.point.domain.port

import com.konkuk.ma.domain.point.domain.balance.MemberPoint

interface MemberPointRepository {
    fun findOneOrInitial(ownerId: Long): MemberPoint

    fun save(memberPoint: MemberPoint): Long

    fun delete(ownerId: Long)
}
