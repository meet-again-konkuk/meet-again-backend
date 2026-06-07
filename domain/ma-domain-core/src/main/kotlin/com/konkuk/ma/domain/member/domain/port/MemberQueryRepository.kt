package com.konkuk.ma.domain.member.domain.port

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.PhoneNumber

interface MemberQueryRepository {
    fun existsByNickname(nickname: String): Boolean
    fun exists(email: Email): Boolean
    fun exists(phoneNumber: PhoneNumber): Boolean
    fun findOne(email: Email): Member
    fun findByNames(names: Set<String>): List<Member>
    fun findByEmails(emails: Set<Email>): List<Member>
}
