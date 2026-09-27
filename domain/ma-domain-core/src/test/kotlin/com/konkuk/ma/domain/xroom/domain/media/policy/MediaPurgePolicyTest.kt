package com.konkuk.ma.domain.xroom.domain.media.policy

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime

class MediaPurgePolicyTest : FunSpec({

    context("purgeCutoff") {

        test("기준일로부터 7일 전 자정을 반환한다") {
            // Given
            val baseDate = LocalDate.of(2026, 6, 17)

            // When
            val cutoff = MediaPurgePolicy.purgeCutoff(baseDate)

            // Then
            cutoff shouldBe LocalDateTime.of(2026, 6, 10, 0, 0)
        }

        test("월 경계를 넘어가도 7일 전 자정으로 계산한다") {
            // Given
            val baseDate = LocalDate.of(2026, 3, 3)

            // When
            val cutoff = MediaPurgePolicy.purgeCutoff(baseDate)

            // Then - 2월로 넘어가고 시각은 항상 00:00
            cutoff shouldBe LocalDateTime.of(2026, 2, 24, 0, 0)
        }
    }
})
