package com.konkuk.ma.domain.member.domain.port

import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.NewMember
import java.time.LocalDateTime

interface MemberCommandRepository {
    fun save(newMember: NewMember): Long
    fun requestWithdrawal(memberId: Long, requestedAt: LocalDateTime = LocalDateTime.now())
    fun cancelWithdrawal(memberId: Long)
    fun updatePassword(memberId: Long, encodedPassword: String)
    fun anonymizeAndSoftDelete(member: Member)
}

