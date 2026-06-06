package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.auth.domain.PasswordVerifier
import com.konkuk.ma.domain.auth.exception.PasswordMismatchException
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.exception.AlreadyWithdrawalRequestedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.time.LocalDateTime

class MemberWithdrawalValidatorTest : FunSpec({

    val passwordVerifier = mockk<PasswordVerifier>()
    val validator = MemberWithdrawalValidator(passwordVerifier)

    beforeEach { clearAllMocks() }

    context("validate") {

        test("활성 회원이고 비밀번호가 일치하면 통과한다") {
            val member = MemberFixture.create()
            every { passwordVerifier.verify("password", member) } just runs

            validator.validate(member, "password")

            verify { passwordVerifier.verify("password", member) }
        }

        test("이미 탈퇴 신청 중이면 AlreadyWithdrawalRequestedException을 던진다") {
            val member = MemberFixture.create()
            member.requestWithdrawal(LocalDateTime.now())

            shouldThrow<AlreadyWithdrawalRequestedException> {
                validator.validate(member, "password")
            }
        }

        test("비밀번호가 일치하지 않으면 PasswordMismatchException을 던진다") {
            val member = MemberFixture.create()
            every { passwordVerifier.verify("wrong", member) } throws PasswordMismatchException(member.email)

            shouldThrow<PasswordMismatchException> {
                validator.validate(member, "wrong")
            }
        }
    }
})
