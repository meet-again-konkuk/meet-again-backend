package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.common.domain.Email

class Members(val data: List<Member>) {

    private val nicknameByEmail: Map<Email, String> by lazy {
        data.associate { it.email to it.nickname }
    }

    fun findOne(email: Email): Member? = data.find { it.email == email }

    fun findOne(id: Long): Member? = data.find { it.id == id }

    fun extractEmails(): Set<Email> = data.map { it.email }.toSet()

    fun findNickname(email: Email): String {
        return nicknameByEmail[email] ?: UNKNOWN_NICKNAME
    }

    companion object {
        private const val UNKNOWN_NICKNAME = "알 수 없음"
    }
}
