package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.domain.xroom.fixture.MemoryFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class MemoriesTest : FunSpec({

    context("sortedByEventDate") {

        test("시점(sortKey) 오름차순으로 정렬한다") {
            val earlier = MemoryFixture.create(
                id = 1L,
                eventDate = EventDate.of(EventDatePrecision.DAY, LocalDate.of(2019, 1, 1)),
            )
            val later = MemoryFixture.create(
                id = 2L,
                eventDate = EventDate.of(EventDatePrecision.DAY, LocalDate.of(2020, 1, 1)),
            )
            val memories = Memories(listOf(later, earlier))

            val sorted = memories.sortedByEventDate()

            sorted.map { it.id } shouldBe listOf(earlier.id, later.id)
        }

        test("정밀도가 달라도 정규화된 sortKey 기준으로 정렬한다") {
            val yearOnly = MemoryFixture.create(
                id = 1L,
                eventDate = EventDate.of(EventDatePrecision.YEAR, LocalDate.of(2019, 1, 1)),
            )
            val sameYearMonth = MemoryFixture.create(
                id = 2L,
                eventDate = EventDate.of(EventDatePrecision.MONTH, LocalDate.of(2019, 6, 1)),
            )
            val memories = Memories(listOf(sameYearMonth, yearOnly))

            val sorted = memories.sortedByEventDate()

            sorted.map { it.id } shouldBe listOf(yearOnly.id, sameYearMonth.id)
        }

        test("동일한 sortKey면 id 오름차순으로 정렬한다") {
            val sameDate = EventDate.of(EventDatePrecision.DAY, LocalDate.of(2019, 5, 10))
            val first = MemoryFixture.create(id = 10L, eventDate = sameDate)
            val second = MemoryFixture.create(id = 20L, eventDate = sameDate)
            val memories = Memories(listOf(second, first))

            val sorted = memories.sortedByEventDate()

            sorted.map { it.id } shouldBe listOf(first.id, second.id)
        }

        test("기억이 없으면 빈 리스트를 반환한다") {
            Memories(emptyList()).sortedByEventDate() shouldBe emptyList()
        }
    }
})
