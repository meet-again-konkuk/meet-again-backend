package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.matching.fixture.TargetInfoFixture
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class TargetInfoTest : FunSpec({

    context("TargetInfo.makeMatchingResults") {

        test("이름과 성별이 일치하는 Target만 매칭된다") {
            val targetInfo = TargetInfoFixture.create(
                targetName = "홍길동",
                targetGender = Gender.MALE
            )

            val targets = Targets.from(listOf(
                MemberFixture.create(name = "김철수", gender = Gender.MALE),
                MemberFixture.create(name = "홍길동", gender = Gender.FEMALE),
                MemberFixture.create(name = "홍길동", gender = Gender.MALE)
            ))

            val results = targetInfo.makeMatchingResults(targets)

            results.data shouldHaveSize 1
        }

        test("모든 비교 필드가 일치하면 모든 matched 플래그가 true") {
            val targetInfo = TargetInfoFixture.create(
                middleNumber = FourDigit("1234"),
                lastNumber = FourDigit("5678"),
                region = Region.SEOUL
            )

            val targets = Targets.from(listOf(
                MemberFixture.create(
                    email = "target@example.com",
                    phoneNumber = "01012345678",
                    birthDate = LocalDate.of(1999, 12, 31),
                    region = Region.SEOUL
                )
            ))

            val results = targetInfo.makeMatchingResults(targets)
            results.data shouldHaveSize 1

            val result = results.data.first()
            result.registerEmail shouldBe Email("register@example.com")
            result.targetEmail shouldBe Email("target@example.com")
            result.middleNumberMatched shouldBe true
            result.lastNumberMatched shouldBe true
            result.regionMatched shouldBe true
        }

        test("일부 필드가 불일치하면 해당 matched 플래그만 false") {
            val targetInfo = TargetInfoFixture.create(
                middleNumber = FourDigit("1234"),
                lastNumber = FourDigit("5678"),
                region = Region.SEOUL
            )

            val targets = Targets.from(listOf(
                MemberFixture.create(
                    phoneNumber = "01012349999",
                    birthDate = LocalDate.of(1999, 1, 31),
                    region = Region.BUSAN
                )
            ))

            val result = targetInfo.makeMatchingResults(targets).data.first()

            result.middleNumberMatched shouldBe true
            result.lastNumberMatched shouldBe false
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

            val targets = Targets.from(listOf(MemberFixture.create()))

            val result = targetInfo.makeMatchingResults(targets).data.first()

            result.middleNumberMatched shouldBe false
            result.lastNumberMatched shouldBe false
            result.yearMatched shouldBe false
            result.monthMatched shouldBe false
            result.dayMatched shouldBe false
            result.regionMatched shouldBe false
        }
    }
})
