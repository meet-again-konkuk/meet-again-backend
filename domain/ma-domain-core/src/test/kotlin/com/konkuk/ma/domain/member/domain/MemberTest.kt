package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.auth.domain.port.PasswordEncryptor
import com.konkuk.ma.domain.member.exception.PasswordMismatchException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class MemberTest : FunSpec({

    context("Member.matches") {
        test("암호가 일치하면 true를 반환한다") {
            // Given
            val encryptor = mockk<PasswordEncryptor>()
            val member = MemberFixture.create()
            val inputPassword = "input-password"

            every { encryptor.matches(inputPassword, member.password) } returns true

            // When
            val result = member.matches(inputPassword, encryptor)

            // Then
            result shouldBe true
        }

        test("암호가 일치하지 않으면 예외를 던진다") {
            // Given
            val encryptor = mockk<PasswordEncryptor>()
            val member = MemberFixture.create()
            val wrongPassword = "wrong-password"

            every { encryptor.matches(wrongPassword, member.password) } returns false

            // When & Then
            val e = shouldThrow<PasswordMismatchException> {
                member.matches(wrongPassword, encryptor)
            }
            e.message shouldBe "비밀번호가 올바르지 않습니다."
        }
    }
})
