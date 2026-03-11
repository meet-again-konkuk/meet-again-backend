package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Day
import com.konkuk.ma.domain.common.domain.Month
import com.konkuk.ma.domain.common.domain.Year
import com.konkuk.ma.domain.matching.fixture.TargetFixture
import com.konkuk.ma.domain.matching.fixture.TargetInfoFixture
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Region
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class TargetInfoTest : FunSpec({

    context("TargetInfo.makeMatchingResults") {

        test("모든 비교 필드가 일치하면 모든 matched 플래그가 true") {
            val middle = FourDigit("1234")
            val last = FourDigit("5678")
            val year = Year(1999)
            val month = Month(12)
            val day = Day(31)
            val region = Region.SEOUL

            val targetInfo = TargetInfoFixture.create(
                middleNumber = middle,
                lastNumber = last,
                year = year,
                month = month,
                day = day,
                region = region
            )

            val target = TargetFixture.create(
                email = "target@example.com",
                middleNumber = middle,
                lastNumber = last,
                year = year,
                month = month,
                day = day,
                region = region
            )

            val results = targetInfo.makeMatchingResults(listOf(target))
            results.data shouldHaveSize 1

            val result = results.data.first()
            result.registerEmail shouldBe "register@example.com"
            result.targetEmail shouldBe "target@example.com"
            result.middleNumberMatched shouldBe true
            result.lastNumberMatched shouldBe true
            result.yearMatched shouldBe true
            result.monthMatched shouldBe true
            result.dayMatched shouldBe true
            result.regionMatched shouldBe true
        }

        test("일부 필드가 불일치하면 해당 matched 플래그만 false") {
            val targetInfo = TargetInfoFixture.create(
                middleNumber = FourDigit("1234"),
                lastNumber = FourDigit("5678"),
                year = Year(1999),
                month = Month(12),
                day = Day(31),
                region = Region.SEOUL
            )

            val target = TargetFixture.create(
                middleNumber = FourDigit("1234"),
                lastNumber = FourDigit("9999"),
                year = Year(1999),
                month = Month(1),
                day = Day(31),
                region = Region.BUSAN
            )

            val result = targetInfo.makeMatchingResults(listOf(target)).data.first()

            result.middleNumberMatched shouldBe true
            result.lastNumberMatched shouldBe false
            result.yearMatched shouldBe true
            result.monthMatched shouldBe false
            result.dayMatched shouldBe true
            result.regionMatched shouldBe false
        }

        test("TargetInfo 쪽 값이 null이면 해당 matched 플래그는 false") {
            val targetInfo = TargetInfoFixture.create(
                middleNumber = null,
                lastNumber = null,
                year = null,
                month = null,
                day = null,
                region = null
            )

            val target = TargetFixture.create(
                middleNumber = FourDigit("1234"),
                lastNumber = FourDigit("5678"),
                year = Year(1999),
                month = Month(12),
                day = Day(31),
                region = Region.SEOUL
            )

            val result = targetInfo.makeMatchingResults(listOf(target)).data.first()

            result.middleNumberMatched shouldBe false
            result.lastNumberMatched shouldBe false
            result.yearMatched shouldBe false
            result.monthMatched shouldBe false
            result.dayMatched shouldBe false
            result.regionMatched shouldBe false
        }

        test("자기 자신과는 매칭되지 않는다") {
            val targetInfo = TargetInfoFixture.create(
                registerEmail = "same@example.com"
            )

            val selfTarget = TargetFixture.create(email = "same@example.com")
            val otherTarget = TargetFixture.create(email = "other@example.com")

            val results = targetInfo.makeMatchingResults(listOf(selfTarget, otherTarget))

            results.data shouldHaveSize 1
            results.data.first().targetEmail shouldBe "other@example.com"
        }
    }
})
