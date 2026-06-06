package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.application.command.WithdrawalRequestCommand
import com.konkuk.ma.domain.auth.domain.PasswordVerifier
import com.konkuk.ma.domain.auth.exception.PasswordMismatchException
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime

class WithdrawalServiceTest : FunSpec({

    val memberQueryRepository = mockk<MemberQueryRepository>()
    val memberCommandRepository = mockk<MemberCommandRepository>()
    val passwordVerifier = mockk<PasswordVerifier>()
    val service = WithdrawalService(memberQueryRepository, memberCommandRepository, passwordVerifier)

    beforeEach { clearAllMocks() }

    context("requestWithdrawal") {

        test("비밀번호 확인 후 withdrawalRequestedAt을 세팅하고 동일한 시각으로 저장한다") {
            val member = MemberFixture.create()
            val command = WithdrawalRequestCommand(member.email.value, "password")

            every { memberQueryRepository.findOne(member.email) } returns member
            every { passwordVerifier.verify(command.password, member) } just runs
            val capturedTime = slot<LocalDateTime>()
            every { memberCommandRepository.requestWithdrawal(member.email, capture(capturedTime)) } just runs

            service.requestWithdrawal(command)

            member.withdrawalRequestedAt shouldNotBe null
            capturedTime.captured shouldNotBe null
            verify { passwordVerifier.verify(command.password, member) }
            verify { memberCommandRepository.requestWithdrawal(member.email, any()) }
        }

        test("비밀번호가 틀리면 PasswordMismatchException을 던지고 저장하지 않는다") {
            val member = MemberFixture.create()
            val command = WithdrawalRequestCommand(member.email.value, "wrong")

            every { memberQueryRepository.findOne(member.email) } returns member
            every { passwordVerifier.verify(command.password, member) } throws PasswordMismatchException(member.email)

            shouldThrow<PasswordMismatchException> {
                service.requestWithdrawal(command)
            }

            member.withdrawalRequestedAt shouldBe null
            verify(exactly = 0) { memberCommandRepository.requestWithdrawal(any(), any()) }
        }
    }
})
