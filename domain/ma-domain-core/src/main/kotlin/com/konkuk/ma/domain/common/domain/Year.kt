package com.konkuk.ma.domain.common.domain

import java.time.LocalDate

class Year(
    val value: Int
) {
    init {
        val currentYear = LocalDate.now().year
        if (value !in 1900..currentYear) {
            throw IllegalArgumentException("Invalid Year. value must be between 1900 and $currentYear. value=$value")
        }
    }
}
