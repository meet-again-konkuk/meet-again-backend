package com.konkuk.ma.domain.common.domain.date

import com.konkuk.ma.exception.InvalidValueException

data class Day(
    val value: Int,
) {
    init {
        if (value !in 1..31) {
            throw InvalidValueException(Day::class, value, "1~31 범위여야 합니다")
        }
    }
}
