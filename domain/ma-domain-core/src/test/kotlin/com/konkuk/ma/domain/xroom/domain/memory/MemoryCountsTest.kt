package com.konkuk.ma.domain.xroom.domain.memory

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MemoryCountsTest : FunSpec({

    context("countOf") {

        test("기억 수가 존재하는 방은 해당 값을 반환한다") {
            val memoryCounts = MemoryCounts(mapOf(1L to 3, 2L to 5))

            memoryCounts.countOf(1L) shouldBe 3
            memoryCounts.countOf(2L) shouldBe 5
        }

        test("기억 수가 없는 방은 0을 반환한다") {
            val memoryCounts = MemoryCounts(mapOf(1L to 3))

            memoryCounts.countOf(999L) shouldBe 0
        }

        test("빈 맵이면 모든 방에 대해 0을 반환한다") {
            val memoryCounts = MemoryCounts(emptyMap())

            memoryCounts.countOf(1L) shouldBe 0
        }
    }

    context("hasNoMemory") {

        test("집계 결과에 키가 아예 없는 방은 기억이 없다고 판단한다") {
            // group by 집계는 기억이 0건인 방을 아예 키로 내려주지 않는다
            val memoryCounts = MemoryCounts(mapOf(1L to 3))

            memoryCounts.hasNoMemory(999L) shouldBe true
        }

        test("빈 맵이면 모든 방을 기억이 없다고 판단한다") {
            val memoryCounts = MemoryCounts(emptyMap())

            memoryCounts.hasNoMemory(1L) shouldBe true
        }

        test("집계된 기억 수가 0이면 기억이 없다고 판단한다") {
            val memoryCounts = MemoryCounts(mapOf(1L to 0))

            memoryCounts.hasNoMemory(1L) shouldBe true
        }

        test("집계된 기억 수가 1 이상이면 기억이 있다고 판단한다") {
            val memoryCounts = MemoryCounts(mapOf(1L to 1, 2L to 5))

            memoryCounts.hasNoMemory(1L) shouldBe false
            memoryCounts.hasNoMemory(2L) shouldBe false
        }
    }
})
