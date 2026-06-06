package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.application.command.WithdrawalCancelCommand
import com.konkuk.ma.domain.auth.domain.PasswordVerifier
import com.konkuk.ma.domain.auth.exception.PasswordMismatchException
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.member.exception.NotWithdrawalRequestedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.time.LocalDateTime

class WithdrawalCancelServiceTest : FunSpec({

    val memberQueryRepository = mockk<MemberQueryRepository>()
    val memberCommandRepository = mockk<MemberCommandRepository>()
    val passwordVerifier = mockk<PasswordVerifier>()
    val service = WithdrawalCancelService(memberQueryRepository, memberCommandRepository, passwordVerifier)

    beforeEach { clearAllMocks() }

    context("cancel") {

        test("신청 상태에서 비밀번호가 일치하면 회원을 복구한다") {
            val member = MemberFixture.create()
            member.requestWithdrawal(LocalDateTime.now())
            val command = WithdrawalCancelCommand(member.email.value, "input-password")

            every { memberQueryRepository.findOne(command.email) } returns member
            every { passwordVerifier.verify(command.password, member) } just runs
            every { memberCommandRepository.cancelWithdrawal(member.email) } just runs

            service.cancel(command)

            member.withdrawalRequestedAt shouldBe null
            verify { memberCommandRepository.cancelWithdrawal(member.email) }
        }

        test("비밀번호가 일치하지 않으면 PasswordMismatchException이 발생한다") {
            val member = MemberFixture.create()
            member.requestWithdrawal(LocalDateTime.now())
            val command = WithdrawalCancelCommand(member.email.value, "wrong-password")

            every { memberQueryRepository.findOne(command.email) } returns member
            every { passwordVerifier.verify(command.password, member) } throws PasswordMismatchException(member.email)

            shouldThrow<PasswordMismatchException> {
                service.cancel(command)
            }
            verify(exactly = 0) { memberCommandRepository.cancelWithdrawal(any()) }
        }

        test("탈퇴 신청 상태가 아니면 NotWithdrawalRequestedException이 발생한다") {
            val member = MemberFixture.create()
            val command = WithdrawalCancelCommand(member.email.value, "input-password")

            every { memberQueryRepository.findOne(command.email) } returns member
            every { passwordVerifier.verify(command.password, member) } just runs

            shouldThrow<NotWithdrawalRequestedException> {
                service.cancel(command)
            }
        }
    }
})
