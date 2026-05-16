package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.common.domain.Email

class Members(val data: List<Member>) {

    private val memberByEmail: Map<Email, Member> by lazy {
        data.associateBy { it.email }
    }

    fun findOne(email: Email): Member? = memberByEmail[email]

    fun findNickname(email: Email): String {
        val member = memberByEmail[email] ?: return UNKNOWN_NICKNAME
        return WithdrawnNicknameMasker.mask(member)
    }

    companion object {
        private const val UNKNOWN_NICKNAME = "알 수 없음"
    }
}
