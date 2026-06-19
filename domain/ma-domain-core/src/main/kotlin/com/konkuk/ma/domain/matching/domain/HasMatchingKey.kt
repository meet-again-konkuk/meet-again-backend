package com.konkuk.ma.domain.matching.domain

interface HasMatchingKey {
    val targetInfoId: Long
    val targetId: Long

    fun createUniqueKey(): Pair<Long, Long> = Pair(targetInfoId, targetId)
}
