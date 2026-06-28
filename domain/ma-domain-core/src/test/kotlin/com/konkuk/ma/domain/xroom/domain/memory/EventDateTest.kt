package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class EventDateTest : FunSpec({

    context("parse") {

        test("YEAR 정밀도는 'yyyy'를 파싱해 1월 1일로 정규화한다") {
            val eventDate = EventDate.parse("2019", EventDatePrecision.YEAR)

            eventDate.precisionName() shouldBe "YEAR"
            eventDate.normalizedDate shouldBe LocalDate.of(2019, 1, 1)
        }

        test("MONTH 정밀도는 'yyyy-MM'을 파싱해 해당 월 1일로 정규화한다") {
            val eventDate = EventDate.parse("2019-05", EventDatePrecision.MONTH)

            eventDate.precisionName() shouldBe "MONTH"
            eventDate.normalizedDate shouldBe LocalDate.of(2019, 5, 1)
        }

        test("DAY 정밀도는 'yyyy-MM-dd'를 파싱해 실제 날짜로 정규화한다") {
            val eventDate = EventDate.parse("2019-05-10", EventDatePrecision.DAY)

            eventDate.precisionName() shouldBe "DAY"
            eventDate.normalizedDate shouldBe LocalDate.of(2019, 5, 10)
        }

        test("YEAR 정밀도인데 'yyyy-MM' 형식이면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                EventDate.parse("2019-05", EventDatePrecision.YEAR)
            }
        }

        test("MONTH 정밀도인데 'yyyy' 형식이면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                EventDate.parse("2019", EventDatePrecision.MONTH)
            }
        }

        test("DAY 정밀도인데 'yyyy-MM' 형식이면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                EventDate.parse("2019-05", EventDatePrecision.DAY)
            }
        }

        test("존재하지 않는 월이면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                EventDate.parse("2019-13", EventDatePrecision.MONTH)
            }
        }

        test("존재하지 않는 날짜면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                EventDate.parse("2019-02-30", EventDatePrecision.DAY)
            }
        }
    }

    context("toWire") {

        test("YEAR 정밀도는 parse한 값을 'yyyy'로 라운드트립한다") {
            val wire = "2019"

            EventDate.parse(wire, EventDatePrecision.YEAR).toWire() shouldBe wire
        }

        test("MONTH 정밀도는 parse한 값을 'yyyy-MM'으로 라운드트립한다") {
            val wire = "2019-05"

            EventDate.parse(wire, EventDatePrecision.MONTH).toWire() shouldBe wire
        }

        test("DAY 정밀도는 parse한 값을 'yyyy-MM-dd'로 라운드트립한다") {
            val wire = "2019-05-10"

            EventDate.parse(wire, EventDatePrecision.DAY).toWire() shouldBe wire
        }
    }

    context("of") {

        test("정밀도와 정규화된 날짜로 복원하면 정밀도에 맞는 wire 값을 반환한다") {
            val normalizedDate = LocalDate.of(2019, 5, 1)

            val eventDate = EventDate.of(EventDatePrecision.MONTH, normalizedDate)

            eventDate.precisionName() shouldBe "MONTH"
            eventDate.normalizedDate shouldBe normalizedDate
            eventDate.toWire() shouldBe "2019-05"
        }
    }
})
