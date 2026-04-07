package com.konkuk.ma.domain.member.domain.port

import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.Members

interface MemberQueryRepository {
    fun existsByNickname(nickname: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun findOne(email: String): Member
    fun findByNames(names: Set<String>): List<Member>
    fun findByEmails(emails: Set<String>): Members
}
