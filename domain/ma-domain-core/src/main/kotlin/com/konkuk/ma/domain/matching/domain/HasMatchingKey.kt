package com.konkuk.ma.domain.matching.domain

interface HasMatchingKey {
    val targetInfoId: Long
    val targetEmail: String

    fun createUniqueKey(): Pair<Long, String> = Pair(targetInfoId, targetEmail)
}
