package com.konkuk.ma.domain.common.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EmailTest : FunSpec({

    context("Email 객체 생성 테스트") {

        test("유효한 이메일로 객체 생성 성공") {
            val email = Email("user@example.com")
            email.value shouldBe "user@example.com"
        }

        test("서브도메인이 포함된 이메일로 객체 생성 성공") {
            val email = Email("user@mail.example.com")
            email.value shouldBe "user@mail.example.com"
        }

        test("특수문자가 포함된 이메일로 객체 생성 성공") {
            val email = Email("user+tag@example.com")
            email.value shouldBe "user+tag@example.com"
        }

        test("빈 문자열로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                Email("")
            }.message shouldBe "이메일은 비어있을 수 없습니다."
        }

        test("공백 문자열로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                Email("   ")
            }.message shouldBe "이메일은 비어있을 수 없습니다."
        }

        test("@가 없는 문자열로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                Email("userexample.com")
            }.message shouldBe "유효하지 않은 이메일 형식입니다: userexample.com"
        }

        test("도메인이 없는 이메일로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                Email("user@")
            }.message shouldBe "유효하지 않은 이메일 형식입니다: user@"
        }

        test("로컬파트가 없는 이메일로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                Email("@example.com")
            }.message shouldBe "유효하지 않은 이메일 형식입니다: @example.com"
        }
    }

    context("toString") {

        test("toString은 이메일 값을 반환한다") {
            val email = Email("user@example.com")
            email.toString() shouldBe "user@example.com"
        }
    }

    context("equals") {

        test("같은 값의 Email은 동일하다") {
            Email("user@example.com") shouldBe Email("user@example.com")
        }
    }
})
