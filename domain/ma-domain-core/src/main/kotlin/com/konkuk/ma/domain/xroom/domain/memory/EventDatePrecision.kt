package com.konkuk.ma.domain.xroom.domain.memory

enum class EventDatePrecision {
    YEAR,
    MONTH,
    DAY,
    ;

    companion object {
        fun from(value: String): EventDatePrecision = valueOf(value.uppercase())
    }
}
