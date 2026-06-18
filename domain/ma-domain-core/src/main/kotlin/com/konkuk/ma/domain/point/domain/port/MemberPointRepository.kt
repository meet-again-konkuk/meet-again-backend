package com.konkuk.ma.domain.point.domain.port

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.point.domain.balance.MemberPoint

interface MemberPointRepository {
    fun findOneOrInitial(ownerEmail: Email): MemberPoint

    fun save(memberPoint: MemberPoint): Long

    fun delete(ownerEmail: Email)
}
