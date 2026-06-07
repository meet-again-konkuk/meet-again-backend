package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.exception.AlreadyWithdrawalRequestedException
import com.konkuk.ma.domain.member.exception.NotWithdrawalRequestedException
import com.konkuk.ma.domain.member.exception.WithdrawalPendingLoginException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class MemberTest : FunSpec({

    context("requestWithdrawal") {

        test("처음 신청하면 withdrawalRequestedAt을 설정하고 시각을 반환한다") {
            val member = MemberFixture.create()
            val now = LocalDateTime.of(2026, 5, 1, 10, 0)

            val requestedAt = member.requestWithdrawal(now)

            requestedAt shouldBe now
            member.withdrawalRequestedAt shouldBe now
        }

        test("이미 신청 상태면 AlreadyWithdrawalRequestedException을 던진다") {
            val member = MemberFixture.create()
            member.requestWithdrawal(LocalDateTime.now())

            shouldThrow<AlreadyWithdrawalRequestedException> {
                member.requestWithdrawal(LocalDateTime.now())
            }
        }
    }

    context("cancelWithdrawal") {

        test("신청 상태에서 호출하면 withdrawalRequestedAt을 null로 만든다") {
            val member = MemberFixture.create()
            member.requestWithdrawal(LocalDateTime.now())

            member.cancelWithdrawal()

            member.withdrawalRequestedAt shouldBe null
        }

        test("신청 상태가 아니면 NotWithdrawalRequestedException을 던진다") {
            val member = MemberFixture.create()

            shouldThrow<NotWithdrawalRequestedException> {
                member.cancelWithdrawal()
            }
        }
    }

    context("verifyLogin") {

        test("활성 회원이면 통과한다") {
            val member = MemberFixture.create()

            shouldNotThrowAny {
                member.verifyLogin()
            }
        }

        test("탈퇴 신청 상태면 WithdrawalPendingLoginException을 던진다") {
            val member = MemberFixture.create()
            member.requestWithdrawal(LocalDateTime.now())

            shouldThrow<WithdrawalPendingLoginException> {
                member.verifyLogin()
            }
        }
    }
})
