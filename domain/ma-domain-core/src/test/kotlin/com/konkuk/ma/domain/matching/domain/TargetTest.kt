package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class TargetTest : FunSpec({

    context("matchesNameAndGender") {

        test("이름과 성별이 모두 일치하면 true를 반환한다") {
            val target = Target.create(
                MemberFixture.create(name = "홍길동", gender = Gender.MALE)
            )

            target.matchesNameAndGender("홍길동", Gender.MALE) shouldBe true
        }

        test("이름이 다르면 false를 반환한다") {
            val target = Target.create(
                MemberFixture.create(name = "홍길동", gender = Gender.MALE)
            )

            target.matchesNameAndGender("김철수", Gender.MALE) shouldBe false
        }

        test("성별이 다르면 false를 반환한다") {
            val target = Target.create(
                MemberFixture.create(name = "홍길동", gender = Gender.MALE)
            )

            target.matchesNameAndGender("홍길동", Gender.FEMALE) shouldBe false
        }

        test("이름과 성별 모두 다르면 false를 반환한다") {
            val target = Target.create(
                MemberFixture.create(name = "홍길동", gender = Gender.MALE)
            )

            target.matchesNameAndGender("김철수", Gender.FEMALE) shouldBe false
        }
    }

    context("create") {

        test("Member로부터 Target을 올바르게 생성한다") {
            val member = MemberFixture.create(
                email = "test@example.com",
                name = "홍길동",
                gender = Gender.MALE,
                phoneNumber = "01012345678",
                birthDate = LocalDate.of(1999, 12, 31),
                region = Region.SEOUL
            )

            val target = Target.create(member)

            target.email shouldBe Email("test@example.com")
            target.name shouldBe "홍길동"
            target.gender shouldBe Gender.MALE
            target.middleNumber shouldBe member.phoneNumber.middleNumber
            target.lastNumber shouldBe member.phoneNumber.lastNumber
            target.year.value shouldBe 1999
            target.month.value shouldBe 12
            target.day.value shouldBe 31
            target.region shouldBe Region.SEOUL
        }
    }
})
