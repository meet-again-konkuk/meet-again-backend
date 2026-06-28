package com.konkuk.ma.domain.member.domain

class Members(val data: List<Member>) {

    private val nicknameById: Map<Long, String> by lazy {
        data.associate { it.id to it.nickname }
    }

    private val nameById: Map<Long, String> by lazy {
        data.associate { it.id to it.name }
    }

    fun findOne(id: Long): Member? = data.find { it.id == id }

    fun extractIds(): Set<Long> = data.map { it.id }.toSet()

    fun findNickname(id: Long): String {
        return nicknameById[id] ?: UNKNOWN
    }

    fun findName(id: Long): String {
        return nameById[id] ?: UNKNOWN
    }

    companion object {
        private const val UNKNOWN = "알 수 없음"
    }
}
