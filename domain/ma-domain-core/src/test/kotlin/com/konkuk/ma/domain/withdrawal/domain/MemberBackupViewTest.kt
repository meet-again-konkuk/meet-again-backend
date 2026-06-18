package com.konkuk.ma.domain.withdrawal.domain

import com.konkuk.ma.domain.matching.fixture.MemberFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import java.time.LocalDateTime

class MemberBackupViewTest : FunSpec({

    context("from") {

        test("전화번호 중간자리를 마스킹한 값으로 변환한다") {
            val member = MemberFixture.create(phoneNumber = "01012345678")

            val view = MemberBackupView.from(member)

            view.maskedPhoneNumber shouldBe "010-****-5678"
        }

        test("마스킹된 전화번호에는 원본 중간자리가 노출되지 않는다") {
            val member = MemberFixture.create(phoneNumber = "01099991234")

            val view = MemberBackupView.from(member)

            view.maskedPhoneNumber shouldNotContain "9999"
        }

        test("회원 식별·근거 필드를 원본 그대로 보존한다") {
            val member = MemberFixture.create(
                id = 7L,
                email = "user@example.com",
                name = "김근거",
                nickname = "근거닉",
                highSchool = "건국고",
                university = "건국대",
            )

            val view = MemberBackupView.from(member)

            view.id shouldBe member.id
            view.email shouldBe member.email.value
            view.name shouldBe member.name
            view.nickname shouldBe member.nickname
            view.gender shouldBe member.gender
            view.region shouldBe member.region
            view.birthDate shouldBe member.birthDate
            view.highSchool shouldBe member.highSchool
            view.university shouldBe member.university
        }

        test("탈퇴 신청 시각을 보존한다") {
            val member = MemberFixture.create()
            val requestedAt = LocalDateTime.of(2026, 6, 1, 10, 0)
            member.requestWithdrawal(requestedAt)

            val view = MemberBackupView.from(member)

            view.withdrawalRequestedAt shouldBe requestedAt
        }
    }
})
