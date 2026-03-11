package com.konkuk.ma.domain.common.domain

data class Day(
    val value: Int,
) {
    init {
        if (value !in 1..31) {
            throw IllegalArgumentException("$value is not a valid day.")
        }
    }
}
