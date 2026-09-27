package com.konkuk.ma.domain.community.domain.image.policy

import java.time.LocalDate
import java.time.LocalDateTime

object PostImagePurgePolicy {
    private const val RETENTION_DAYS = 7L

    fun purgeCutoff(baseDate: LocalDate): LocalDateTime =
        baseDate.minusDays(RETENTION_DAYS).atStartOfDay()
}
