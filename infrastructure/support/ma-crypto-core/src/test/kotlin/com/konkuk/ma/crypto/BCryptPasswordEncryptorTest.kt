package com.konkuk.ma.crypto

import com.konkuk.ma.BCryptPasswordEncryptor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class BCryptPasswordEncryptorTest : FunSpec({

    val passwordEncryptor = BCryptPasswordEncryptor()

    context("encode") {
        test("비밀번호를 암호화한다") {
            // Given
            val rawPassword = "password123"

            // When
            val encodedPassword = passwordEncryptor.encode(rawPassword)

            // Then
            encodedPassword shouldNotBe rawPassword
            encodedPassword.length shouldBe 60 // BCrypt 해시 길이
        }

        test("같은 비밀번호라도 매번 다른 해시를 생성한다") {
            // Given
            val rawPassword = "password123"

            // When
            val encodedPassword1 = passwordEncryptor.encode(rawPassword)
            val encodedPassword2 = passwordEncryptor.encode(rawPassword)

            // Then
            encodedPassword1 shouldNotBe encodedPassword2
            passwordEncryptor.matches(rawPassword, encodedPassword1) shouldBe true
            passwordEncryptor.matches(rawPassword, encodedPassword2) shouldBe true
        }
    }

    context("matches") {
        test("원본 비밀번호와 암호화된 비밀번호가 일치하는지 확인한다") {
            // Given
            val rawPassword = "password123"
            val encodedPassword = passwordEncryptor.encode(rawPassword)

            // When
            val matches = passwordEncryptor.matches(rawPassword, encodedPassword)

            // Then
            matches shouldBe true
        }

        test("다른 비밀번호는 일치하지 않는다") {
            // Given
            val rawPassword = "password123"
            val wrongPassword = "wrongpassword"
            val encodedPassword = passwordEncryptor.encode(rawPassword)

            // When
            val matches = passwordEncryptor.matches(wrongPassword, encodedPassword)

            // Then
            matches shouldBe false
        }
    }
})
