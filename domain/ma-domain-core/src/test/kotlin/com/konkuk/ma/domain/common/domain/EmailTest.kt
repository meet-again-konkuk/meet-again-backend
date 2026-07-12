package com.konkuk.ma.domain.common.domain

import com.konkuk.ma.exception.InvalidValueException
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
            shouldThrow<InvalidValueException> {
                Email("")
            }
        }

        test("공백 문자열로 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                Email("   ")
            }
        }

        test("@가 없는 문자열로 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                Email("userexample.com")
            }
        }

        test("도메인이 없는 이메일로 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                Email("user@")
            }
        }

        test("로컬파트가 없는 이메일로 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                Email("@example.com")
            }
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

    context("withdrawn") {

        test("회원 id로 익명화 이메일을 생성한다") {
            Email.withdrawn(42L) shouldBe Email("withdrawn_42@deleted.local")
        }
    }

    context("masked") {

        test("로컬파트가 4글자 이상이면 앞 3글자만 남기고 나머지를 마스킹한다") {
            Email("holeman@naver.com").masked() shouldBe "hol***@naver.com"
        }

        test("로컬파트가 정확히 4글자면 앞 3글자를 남긴다") {
            Email("hole@naver.com").masked() shouldBe "hol***@naver.com"
        }

        test("로컬파트가 정확히 3글자면 앞 1글자만 남긴다") {
            Email("abc@naver.com").masked() shouldBe "a***@naver.com"
        }

        test("로컬파트가 2글자면 앞 1글자만 남긴다") {
            Email("ab@x.com").masked() shouldBe "a***@x.com"
        }

        test("로컬파트가 1글자면 앞 1글자만 남긴다") {
            Email("a@x.com").masked() shouldBe "a***@x.com"
        }

        test("도메인은 마스킹하지 않고 서브도메인까지 그대로 유지한다") {
            Email("holeman@mail.example.com").masked() shouldBe "hol***@mail.example.com"
        }
    }
})
