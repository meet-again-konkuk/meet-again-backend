package com.konkuk.ma.domain.common.domain

import com.konkuk.ma.domain.common.exception.InvalidValueException

data class Month(
    val value: Int,
) {
    init {
        if (value !in 1..12) {
            throw InvalidValueException(Month::class, value, "1~12 범위여야 합니다")
        }
    }
}
