package com.konkuk.ma.member.domain.port

import com.konkuk.ma.member.domain.Member

interface MemberQueryRepository {
    fun existsByNickname(nickname: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun findByEmail(email: String): Member?
}
