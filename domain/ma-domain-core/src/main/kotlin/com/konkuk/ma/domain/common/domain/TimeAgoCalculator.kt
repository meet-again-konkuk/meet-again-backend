package com.konkuk.ma.domain.common.domain

import java.time.Duration
import java.time.LocalDateTime

object TimeAgoCalculator {
    fun calculate(createdDate: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
        val duration = Duration.between(createdDate, now)
        return when {
            duration.toMinutes() < 1 -> "방금 전"
            duration.toHours() < 1 -> "${duration.toMinutes()}분 전"
            duration.toDays() < 1 -> "${duration.toHours()}시간 전"
            duration.toDays() < 30 -> "${duration.toDays()}일 전"
            duration.toDays() < 365 -> "${duration.toDays() / 30}개월 전"
            else -> "${duration.toDays() / 365}년 전"
        }
    }
}
