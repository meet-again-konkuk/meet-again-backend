package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.exception.InvalidValueException
import java.time.LocalDate

private const val SEGMENT_DELIMITER = "-"
private const val FIRST_MONTH = 1
private const val FIRST_DAY = 1

enum class EventDatePrecision(private val segmentCount: Int) {
    YEAR(1) {
        override fun format(date: LocalDate): String = "%04d".format(date.year)

        override fun buildDate(segments: List<String>): LocalDate =
            LocalDate.of(segments[0].toInt(), FIRST_MONTH, FIRST_DAY)
    },
    MONTH(2) {
        override fun format(date: LocalDate): String = "%04d-%02d".format(date.year, date.monthValue)

        override fun buildDate(segments: List<String>): LocalDate =
            LocalDate.of(segments[0].toInt(), segments[1].toInt(), FIRST_DAY)
    },
    DAY(3) {
        override fun format(date: LocalDate): String =
            "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)

        override fun buildDate(segments: List<String>): LocalDate =
            LocalDate.of(segments[0].toInt(), segments[1].toInt(), segments[2].toInt())
    },
    ;

    abstract fun format(date: LocalDate): String

    protected abstract fun buildDate(segments: List<String>): LocalDate

    fun parse(value: String): LocalDate {
        val segments = value.split(SEGMENT_DELIMITER)
        if (segments.size != segmentCount) {
            throw InvalidValueException(EventDatePrecision::class, value, "$name 정밀도 형식이 아닙니다.")
        }
        return runCatching { buildDate(segments) }
            .getOrElse { throw InvalidValueException(EventDatePrecision::class, value, "유효하지 않은 날짜입니다.") }
    }

    companion object {
        fun from(value: String): EventDatePrecision {
            return runCatching { valueOf(value.uppercase()) }
                .getOrElse {
                    throw InvalidValueException(EventDatePrecision::class, value, "지원하지 않는 정밀도입니다.")
                }
        }
    }
}
