package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.member.domain.port.PasswordEncryptor
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class MemberTest : FunSpec({

    context("Member.matches") {
        test("암호가 일치하면 true를 반환한다") {
            val encryptor = mockk<PasswordEncryptor>()
            val member = Member.create(
                email = "test@example.com",
                password = "stored-password",
                nickname = "tester",
                phoneNumber = "01012345678",
                region = Region.SEOUL,
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                highSchool = null,
                university = null
            )

            every { encryptor.matches("input-password", "stored-password") } returns true

            member.matches("input-password", encryptor) shouldBe true
        }

        test("암호가 일치하지 않으면 예외를 던진다") {
            val encryptor = mockk<PasswordEncryptor>()
            val member = Member.create(
                email = "test@example.com",
                password = "stored-password",
                nickname = "tester",
                phoneNumber = "01012345678",
                region = Region.SEOUL,
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                highSchool = null,
                university = null
            )

            every { encryptor.matches("wrong-password", "stored-password") } returns false

            val e = shouldThrow<IllegalArgumentException> {
                member.matches("wrong-password", encryptor)
            }
            e.message shouldBe "비밀번호가 올바르지 않습니다."
        }
    }
})
