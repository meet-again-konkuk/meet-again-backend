package com.konkuk.ma.domain.common.domain.file

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class StoragePathTest : FunSpec({

    context("of") {
        test("domain, usage, email을 조합하여 경로를 생성한다") {
            // Given
            val domain = StorageDomainType.MEMBER
            val usage = StorageUsageType.PROFILE
            val email = "test@example.com"

            // When
            val path = StoragePath.of(domain, usage, email)

            // Then
            path.value shouldBe "member/profile/test@example.com"
        }

        test("다른 도메인 타입으로 경로를 생성한다") {
            // Given
            val domain = StorageDomainType.MATCHING
            val usage = StorageUsageType.PROFILE
            val email = "user@example.com"

            // When
            val path = StoragePath.of(domain, usage, email)

            // Then
            path.value shouldBe "matching/profile/user@example.com"
        }
    }

    context("withDate") {
        test("domain, usage, email, date를 조합하여 경로를 생성한다") {
            // Given
            val domain = StorageDomainType.MEMBER
            val usage = StorageUsageType.PROFILE
            val email = "test@example.com"
            val date = LocalDate.of(2025, 1, 15)

            // When
            val path = StoragePath.withDate(domain, usage, email, date)

            // Then
            path.value shouldBe "member/profile/test@example.com/2025-01-15"
        }

        test("date를 생략하면 오늘 날짜가 사용된다") {
            // Given
            val domain = StorageDomainType.MEMBER
            val usage = StorageUsageType.PROFILE
            val email = "test@example.com"
            val today = LocalDate.now()

            // When
            val path = StoragePath.withDate(domain, usage, email)

            // Then
            path.value shouldBe "member/profile/test@example.com/$today"
        }
    }
})
