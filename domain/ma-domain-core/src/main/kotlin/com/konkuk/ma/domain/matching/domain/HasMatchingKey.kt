package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Email

interface HasMatchingKey {
    val targetInfoId: Long
    val targetEmail: Email

    fun createUniqueKey(): Pair<Long, Email> = Pair(targetInfoId, targetEmail)
}
