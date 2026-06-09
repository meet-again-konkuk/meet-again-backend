package com.konkuk.ma.domain.member.domain.port

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.NewMember
import java.time.LocalDateTime

interface MemberCommandRepository {
    fun save(newMember: NewMember): Long
    fun requestWithdrawal(email: Email, requestedAt: LocalDateTime = LocalDateTime.now())
    fun cancelWithdrawal(email: Email)
    fun anonymizeAndSoftDelete(member: Member)
}

