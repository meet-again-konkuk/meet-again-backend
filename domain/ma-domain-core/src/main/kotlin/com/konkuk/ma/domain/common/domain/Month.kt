package com.konkuk.ma.domain.common.domain

data class Month(
    val value: Int,
) {
    init {
        if (value !in 1..12) {
            throw IllegalArgumentException("Invalid month. Value must be in range 1..12. value=$value")
        }
    }
}
