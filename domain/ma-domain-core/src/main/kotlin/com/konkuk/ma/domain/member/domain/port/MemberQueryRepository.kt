package com.konkuk.ma.domain.member.domain.port

import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Member

interface MemberQueryRepository {
    fun existsByNickname(nickname: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun findByEmail(email: String): Member
    fun findByNameAndGender(name: String, gender: Gender): List<Member>
}
