package com.konkuk.ma.domain.member.domain

class Members(val data: List<Member>) {

    private val nicknameByEmail: Map<String, String> by lazy {
        data.associate { it.email to it.nickname }
    }

    fun findOne(email: String): Member? = data.find { it.email == email }

    fun findNicknameByEmail(email: String): String {
        return nicknameByEmail[email] ?: UNKNOWN_NICKNAME
    }

    companion object {
        private const val UNKNOWN_NICKNAME = "알 수 없음"
    }
}
