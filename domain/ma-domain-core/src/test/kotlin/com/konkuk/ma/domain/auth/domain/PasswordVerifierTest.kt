package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.auth.domain.port.PasswordEncryptor
import com.konkuk.ma.domain.auth.exception.PasswordMismatchException
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class PasswordVerifierTest : FunSpec({

    context("PasswordVerifier.verify") {
        test("암호가 일치하면 예외를 던지지 않는다") {
            // Given
            val encryptor = mockk<PasswordEncryptor>()
            val verifier = PasswordVerifier(encryptor)
            val member = MemberFixture.create()
            val inputPassword = "input-password"

            every { encryptor.matches(inputPassword, member.password) } returns true

            // When & Then
            shouldNotThrow<PasswordMismatchException> {
                verifier.verify(inputPassword, member)
            }
        }

        test("암호가 일치하지 않으면 예외를 던진다") {
            // Given
            val encryptor = mockk<PasswordEncryptor>()
            val verifier = PasswordVerifier(encryptor)
            val member = MemberFixture.create()
            val wrongPassword = "wrong-password"

            every { encryptor.matches(wrongPassword, member.password) } returns false

            // When & Then
            val e = shouldThrow<PasswordMismatchException> {
                verifier.verify(wrongPassword, member)
            }
            e.message shouldBe "비밀번호가 올바르지 않습니다."
        }
    }
})
