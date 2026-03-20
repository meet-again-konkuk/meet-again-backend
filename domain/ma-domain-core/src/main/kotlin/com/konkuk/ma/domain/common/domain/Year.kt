package com.konkuk.ma.domain.common.domain

import com.konkuk.ma.domain.common.exception.InvalidValueException
import java.time.LocalDate

data class Year(
    val value: Int
) {
    init {
        val currentYear = LocalDate.now().year
        if (value !in 1900..currentYear) {
            throw InvalidValueException(Year::class, value, "1900~$currentYear 범위여야 합니다")
        }
    }
}
