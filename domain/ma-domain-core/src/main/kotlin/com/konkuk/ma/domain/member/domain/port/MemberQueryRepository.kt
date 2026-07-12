package com.konkuk.ma.domain.member.domain.port

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.PhoneNumber
import java.time.LocalDateTime

interface MemberQueryRepository {
    fun existsByNickname(nickname: String): Boolean
    fun exists(email: Email): Boolean
    fun exists(phoneNumber: PhoneNumber): Boolean
    fun findOne(email: Email): Member
    fun findOne(id: Long): Member
    fun findOne(name: String, phoneNumber: PhoneNumber): Member
    fun findByNames(names: Set<String>): List<Member>
    fun findByIds(ids: Set<Long>): List<Member>
    fun findExpiredWithdrawalRequests(expiredBefore: LocalDateTime, cursorId: Long?, pageSize: Int): List<Member>
}
