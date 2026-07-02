package com.konkuk.ma.domain.xroom.domain.media.policy

import java.time.LocalDate
import java.time.LocalDateTime

object MediaPurgePolicy {
    private const val RETENTION_DAYS = 7L

    fun purgeCutoff(baseDate: LocalDate): LocalDateTime =
        baseDate.minusDays(RETENTION_DAYS).atStartOfDay()
}
