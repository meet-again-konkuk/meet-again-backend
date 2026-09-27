package com.konkuk.ma.domain.community.domain

class LikeCounts(private val countById: Map<Long, Int>) {

    fun countOf(id: Long): Int = countById[id] ?: 0

    companion object {
        fun from(countById: Map<Long, Int>): LikeCounts = LikeCounts(countById)
    }
}
