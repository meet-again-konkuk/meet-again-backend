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
})
