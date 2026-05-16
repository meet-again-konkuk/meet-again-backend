package com.konkuk.ma.domain.member.domain.policy

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WithdrawnSentinelTest : FunSpec({

    context("nickname") {

        test("회원 ID로 탈퇴 닉네임을 만든다") {
            WithdrawnSentinel.nickname(42L) shouldBe "탈퇴한회원_42"
        }
    }

    context("상수 검증") {

        test("PASSWORD는 빈 문자열이다") {
            WithdrawnSentinel.PASSWORD shouldBe ""
        }

        test("NAME은 '탈퇴한회원'이다") {
            WithdrawnSentinel.NAME shouldBe "탈퇴한회원"
        }

        test("PHONE_NUMBER는 010 + 8자리 0이다") {
            WithdrawnSentinel.PHONE_NUMBER shouldBe "01000000000"
        }
    }
})
