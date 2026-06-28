package com.konkuk.ma.domain.xroom.domain.memory

import java.time.LocalDate

class EventDate private constructor(
    private val precision: EventDatePrecision,
    val normalizedDate: LocalDate,
) {
    fun toWire(): String = precision.format(normalizedDate)

    fun precisionName(): String = precision.name

    companion object {
        fun parse(value: String, precision: EventDatePrecision): EventDate {
            return EventDate(precision, precision.parse(value))
        }

        fun of(precision: EventDatePrecision, normalizedDate: LocalDate): EventDate {
            return EventDate(precision, normalizedDate)
        }
    }
}
