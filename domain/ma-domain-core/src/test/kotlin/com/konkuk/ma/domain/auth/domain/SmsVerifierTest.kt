package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.auth.domain.port.SmsRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class SmsVerifierTest : FunSpec({

    val smsRepository = mockk<SmsRepository>()
    val smsVerifier = SmsVerifier(smsRepository)

    context("verify") {

        test("인증 코드가 일치하면 true를 반환하고 confirmVerificationCode를 호출한다") {
            // Given
            val phoneNumber = "01012345678"
            val verificationCode = 123456

            every { smsRepository.findOrNull(phoneNumber) } returns verificationCode
            every { smsRepository.confirmVerificationCode(phoneNumber) } returns Unit

            // When
            val result = smsVerifier.verify(phoneNumber, verificationCode)

            // Then
            result.shouldBeTrue()
            verify { smsRepository.confirmVerificationCode(phoneNumber) }
        }

        test("인증 코드가 불일치하면 false를 반환한다") {
            // Given
            val phoneNumber = "01012345678"
            val storedCode = 123456
            val wrongCode = 999999

            every { smsRepository.findOrNull(phoneNumber) } returns storedCode

            // When
            val result = smsVerifier.verify(phoneNumber, wrongCode)

            // Then
            result.shouldBeFalse()
        }

        test("저장된 인증 코드가 없으면 false를 반환한다") {
            // Given
            val phoneNumber = "01012345678"
            val inputCode = 123456

            every { smsRepository.findOrNull(phoneNumber) } returns null

            // When
            val result = smsVerifier.verify(phoneNumber, inputCode)

            // Then
            result.shouldBeFalse()
        }
    }
})
