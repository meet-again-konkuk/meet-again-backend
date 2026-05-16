package com.konkuk.ma.domain.member.application

import com.konkuk.ma.domain.auth.domain.AuthTokenIssuer
import com.konkuk.ma.domain.auth.domain.AuthTokens
import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.MemberWithdrawalCancelValidator
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.time.LocalDateTime

class MemberWithdrawalCancelServiceTest : FunSpec({

    val memberQueryRepository = mockk<MemberQueryRepository>()
    val memberCommandRepository = mockk<MemberCommandRepository>()
    val cancelValidator = mockk<MemberWithdrawalCancelValidator>()
    val authTokenIssuer = mockk<AuthTokenIssuer>()
    val service = MemberWithdrawalCancelService(
        memberQueryRepository, memberCommandRepository, cancelValidator, authTokenIssuer
    )

    beforeEach { clearAllMocks() }

    context("cancel") {

        test("검증 통과 시 Member 복구 + 새 토큰 발급한 결과를 반환한다") {
            val member = MemberFixture.create()
            member.requestWithdrawal(LocalDateTime.now())
            val accessToken = "new-access"
            val refreshToken = RefreshToken(member.email, LocalDateTime.now().plusDays(7), "new-refresh")
            val authTokens = AuthTokens(accessToken, refreshToken)

            every { memberQueryRepository.findOne(member.email) } returns member
            every { cancelValidator.validate(member, any()) } just runs
            every { memberCommandRepository.cancelWithdrawal(member.email) } just runs
            every { authTokenIssuer.issueFor(member.email) } returns authTokens

            val result = service.cancel(member.email.value)

            result.loginInfo.email shouldBe member.email
            result.loginInfo.nickname shouldBe member.nickname
            result.loginInfo.accessToken shouldBe accessToken
            result.loginInfo.refreshToken shouldBe refreshToken
            member.withdrawalRequestedAt shouldBe null
            verify { memberCommandRepository.cancelWithdrawal(member.email) }
        }
    }
})
