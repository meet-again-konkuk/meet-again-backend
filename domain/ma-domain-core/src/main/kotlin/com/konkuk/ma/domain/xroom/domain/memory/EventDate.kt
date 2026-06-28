package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.exception.InvalidValueException
import java.time.LocalDate

class EventDate private constructor(
    private val precision: EventDatePrecision,
    val normalizedDate: LocalDate,
) {
    fun toWire(): String {
        return when (precision) {
            EventDatePrecision.YEAR -> "%04d".format(normalizedDate.year)
            EventDatePrecision.MONTH -> "%04d-%02d".format(normalizedDate.year, normalizedDate.monthValue)
            EventDatePrecision.DAY ->
                "%04d-%02d-%02d".format(normalizedDate.year, normalizedDate.monthValue, normalizedDate.dayOfMonth)
        }
    }

    fun precisionName(): String = precision.name

    companion object {
        private const val YEAR_SEGMENTS = 1
        private const val MONTH_SEGMENTS = 2
        private const val DAY_SEGMENTS = 3
        private const val FIRST_MONTH = 1
        private const val FIRST_DAY = 1
        private const val SEGMENT_DELIMITER = "-"

        fun parse(value: String, precision: EventDatePrecision): EventDate {
            val segments = value.split(SEGMENT_DELIMITER)
            val normalizedDate = when (precision) {
                EventDatePrecision.YEAR -> parseYear(value, segments)
                EventDatePrecision.MONTH -> parseMonth(value, segments)
                EventDatePrecision.DAY -> parseDay(value, segments)
            }
            return EventDate(precision, normalizedDate)
        }

        fun of(precision: EventDatePrecision, normalizedDate: LocalDate): EventDate {
            return EventDate(precision, normalizedDate)
        }

        private fun parseYear(value: String, segments: List<String>): LocalDate {
            requireSegments(value, segments, YEAR_SEGMENTS, "연도는 'yyyy' 형식이어야 합니다.")
            return toLocalDate(value, segments[0], FIRST_MONTH.toString(), FIRST_DAY.toString())
        }

        private fun parseMonth(value: String, segments: List<String>): LocalDate {
            requireSegments(value, segments, MONTH_SEGMENTS, "연월은 'yyyy-MM' 형식이어야 합니다.")
            return toLocalDate(value, segments[0], segments[1], FIRST_DAY.toString())
        }

        private fun parseDay(value: String, segments: List<String>): LocalDate {
            requireSegments(value, segments, DAY_SEGMENTS, "연월일은 'yyyy-MM-dd' 형식이어야 합니다.")
            return toLocalDate(value, segments[0], segments[1], segments[2])
        }

        private fun requireSegments(value: String, segments: List<String>, expected: Int, reason: String) {
            if (segments.size != expected) {
                throw InvalidValueException(EventDate::class, value, reason)
            }
        }

        private fun toLocalDate(value: String, year: String, month: String, day: String): LocalDate {
            return runCatching { LocalDate.of(year.toInt(), month.toInt(), day.toInt()) }
                .getOrElse { throw InvalidValueException(EventDate::class, value, "유효하지 않은 날짜입니다.") }
        }
    }
}
