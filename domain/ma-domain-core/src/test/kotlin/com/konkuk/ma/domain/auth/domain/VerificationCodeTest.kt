package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldMatch

class VerificationCodeTest : FunSpec({

    context("VerificationCode 객체 생성 테스트") {

        test("최소값 000000으로 객체 생성 성공") {
            VerificationCode("000000").value shouldBe "000000"
        }

        test("최대값 999999로 객체 생성 성공") {
            VerificationCode("999999").value shouldBe "999999"
        }

        test("0으로 시작하는 코드(012345) 생성 성공") {
            VerificationCode("012345").value shouldBe "012345"
        }

        test("범위 중간값으로 객체 생성 성공") {
            VerificationCode("500000").value shouldBe "500000"
        }

        test("6자리 미만이면 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                VerificationCode("12345")
            }
        }

        test("6자리 초과면 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                VerificationCode("1234567")
            }
        }

        test("숫자가 아닌 문자가 포함되면 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                VerificationCode("12345a")
            }
        }

        test("빈 문자열이면 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                VerificationCode("")
            }
        }
    }

    context("random") {

        test("random은 항상 6자리 숫자 문자열을 반환한다") {
            repeat(1000) {
                val code = VerificationCode.random()
                code.value shouldHaveLength 6
                code.value shouldMatch Regex("^\\d{6}$")
            }
        }
    }

    context("toString") {

        test("toString은 코드 값을 그대로 반환한다") {
            VerificationCode("123456").toString() shouldBe "123456"
        }

        test("0으로 시작하는 코드도 그대로 반환한다") {
            VerificationCode("012345").toString() shouldBe "012345"
        }
    }

    context("equals") {

        test("같은 값의 VerificationCode는 동일하다") {
            VerificationCode("123456") shouldBe VerificationCode("123456")
        }
    }
})
