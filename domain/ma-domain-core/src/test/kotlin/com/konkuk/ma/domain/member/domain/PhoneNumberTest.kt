package com.konkuk.ma.domain.member.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PhoneNumberTest : FunSpec({

    context("PhoneNumber 객체 생성 테스트") {
        test("유효한 11자리 휴대폰 번호(010)로 객체 생성 성공") {
            val phoneNumber = PhoneNumber("01012345678")

            phoneNumber.firstNumber shouldBe "010"
            phoneNumber.middleNumber shouldBe FourDigit("1234")
            phoneNumber.lastNumber shouldBe FourDigit("5678")
            phoneNumber.formatted shouldBe "010-1234-5678"
        }

        test("10자리 휴대폰 번호는 중간번호가 3자리이므로 FourDigit 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumber("0101234567")
            }.message shouldBe "FourDigit는 4자리여야 합니다. value=123"
        }

        test("하이픈이 포함된 휴대폰 번호로 객체 생성 성공") {
            val phoneNumber = PhoneNumber("010-1234-5678")

            phoneNumber.firstNumber shouldBe "010"
            phoneNumber.middleNumber shouldBe FourDigit("1234")
            phoneNumber.lastNumber shouldBe FourDigit("5678")
            phoneNumber.formatted shouldBe "010-1234-5678"
        }

        test("공백이 포함된 휴대폰 번호로 객체 생성 성공") {
            val phoneNumber = PhoneNumber("010 1234 5678")

            phoneNumber.firstNumber shouldBe "010"
            phoneNumber.middleNumber shouldBe FourDigit("1234")
            phoneNumber.lastNumber shouldBe FourDigit("5678")
            phoneNumber.formatted shouldBe "010-1234-5678"
        }

        test("하이픈과 공백이 혼합된 휴대폰 번호로 객체 생성 성공") {
            val phoneNumber = PhoneNumber("010- 1234 -5678")

            phoneNumber.firstNumber shouldBe "010"
            phoneNumber.middleNumber shouldBe FourDigit("1234")
            phoneNumber.lastNumber shouldBe FourDigit("5678")
            phoneNumber.formatted shouldBe "010-1234-5678"
        }

        test("허용되지 않는 앞자리(011)로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumber("01112345678")
            }.message shouldBe "앞자리는 010만 허용됩니다."
        }

        test("허용되지 않는 앞자리(016)로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumber("01612345678")
            }.message shouldBe "앞자리는 010만 허용됩니다."
        }

        test("허용되지 않는 앞자리(017)로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumber("01712345678")
            }.message shouldBe "앞자리는 010만 허용됩니다."
        }

        test("허용되지 않는 앞자리(018)로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumber("01812345678")
            }.message shouldBe "앞자리는 010만 허용됩니다."
        }

        test("허용되지 않는 앞자리(019)로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumber("01912345678")
            }.message shouldBe "앞자리는 010만 허용됩니다."
        }

        test("허용되지 않는 앞자리(070)로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumber("07012345678")
            }.message shouldBe "앞자리는 010만 허용됩니다."
        }

        test("허용되지 않는 앞자리(012)로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumber("01212345678")
            }.message shouldBe "앞자리는 010만 허용됩니다."
        }

        test("허용되지 않는 앞자리(020)로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumber("02012345678")
            }.message shouldBe "앞자리는 010만 허용됩니다."
        }

        test("허용되지 않는 앞자리(031)로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumber("03112345678")
            }.message shouldBe "앞자리는 010만 허용됩니다."
        }

        test("10자리 미만 휴대폰 번호로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumber("010123456")
            }.message shouldBe "전화번호는 최소 10자리(3-중간-4)여야 합니다."
        }

        test("9자리 휴대폰 번호로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumber("01012345")
            }.message shouldBe "전화번호는 최소 10자리(3-중간-4)여야 합니다."
        }

        test("빈 문자열로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumber("")
            }.message shouldBe "전화번호는 최소 10자리(3-중간-4)여야 합니다."
        }

        test("하이픈만 있는 문자열로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumber("---")
            }.message shouldBe "전화번호는 최소 10자리(3-중간-4)여야 합니다."
        }

        test("공백만 있는 문자열로 객체 생성 실패") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumber("      ")
            }.message shouldBe "전화번호는 최소 10자리(3-중간-4)여야 합니다."
        }
    }

    context("formatted 속성 테스트") {
        test("formatted는 하이픈으로 구분된 형식을 반환한다") {
            val phoneNumber = PhoneNumber("01098765432")

            phoneNumber.formatted shouldBe "010-9876-5432"
        }

        test("원본에 하이픈이 있어도 정규화된 형식을 반환한다") {
            val phoneNumber = PhoneNumber("010-9876-5432")

            phoneNumber.formatted shouldBe "010-9876-5432"
        }
    }
})

