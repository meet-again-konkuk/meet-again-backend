package com.konkuk.ma.domain.member.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.application.command.WithdrawalRequestCommand
import com.konkuk.ma.domain.member.domain.MemberWithdrawalValidator
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime

class MemberWithdrawalServiceTest : FunSpec({

    val memberQueryRepository = mockk<MemberQueryRepository>()
    val memberCommandRepository = mockk<MemberCommandRepository>()
    val memberWithdrawalValidator = mockk<MemberWithdrawalValidator>()
    val service = MemberWithdrawalService(memberQueryRepository, memberCommandRepository, memberWithdrawalValidator)

    beforeEach { clearAllMocks() }

    context("requestWithdrawal") {

        test("검증 통과 시 Member의 withdrawalRequestedAt을 세팅하고 동일한 시각으로 저장한다") {
            val member = MemberFixture.create()
            val command = WithdrawalRequestCommand(member.email.value, "password")

            every { memberQueryRepository.findOne(member.email) } returns member
            every { memberWithdrawalValidator.validate(member, command.password) } just runs
            val capturedTime = slot<LocalDateTime>()
            every { memberCommandRepository.requestWithdrawal(member.email, capture(capturedTime)) } just runs

            service.requestWithdrawal(command)

            member.withdrawalRequestedAt shouldNotBe null
            capturedTime.captured shouldNotBe null
            verify { memberWithdrawalValidator.validate(member, command.password) }
            verify { memberCommandRepository.requestWithdrawal(member.email, any()) }
        }
    }
})
