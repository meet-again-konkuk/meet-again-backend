package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.exception.NotWithdrawalRequestedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import java.time.LocalDateTime

class MemberWithdrawalCancelValidatorTest : FunSpec({

    val validator = MemberWithdrawalCancelValidator()

    context("validate") {

        test("탈퇴 신청 상태면 통과한다") {
            val member = MemberFixture.create()
            member.requestWithdrawal(LocalDateTime.now())

            validator.validate(member)
        }

        test("탈퇴 신청 상태가 아니면 NotWithdrawalRequestedException을 던진다") {
            val member = MemberFixture.create()

            shouldThrow<NotWithdrawalRequestedException> {
                validator.validate(member)
            }
        }
    }
})
