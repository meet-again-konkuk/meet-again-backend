package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.common.domain.Email

class Members(val data: List<Member>) {

    private val nicknameById: Map<Long, String> by lazy {
        data.associate { it.id to it.nickname }
    }

    fun findOne(email: Email): Member? = data.find { it.email == email }

    fun findOne(id: Long): Member? = data.find { it.id == id }

    fun extractIds(): Set<Long> = data.map { it.id }.toSet()

    fun findNickname(id: Long): String {
        return nicknameById[id] ?: UNKNOWN_NICKNAME
    }

    companion object {
        private const val UNKNOWN_NICKNAME = "알 수 없음"
    }
}
