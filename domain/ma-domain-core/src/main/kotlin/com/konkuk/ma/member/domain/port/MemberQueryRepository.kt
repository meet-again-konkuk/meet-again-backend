package com.konkuk.ma.member.domain.port

interface MemberQueryRepository {
    fun existsByNickname(nickname: String): Boolean
    fun existsByEmail(email: String): Boolean
}
