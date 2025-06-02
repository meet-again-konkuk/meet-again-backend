package com.konkuk.ma.member.domain

interface MemberQueryRepository {
    fun existsByNickname(nickname: String): Boolean
}
