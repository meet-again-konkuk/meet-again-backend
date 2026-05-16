package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.policy.WithdrawalPolicy
import com.konkuk.ma.domain.member.domain.policy.WithdrawnSentinel
import com.konkuk.ma.domain.member.exception.AlreadyWithdrawalRequestedException
import com.konkuk.ma.domain.member.exception.NotWithdrawalRequestedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime

class MemberWithdrawalTest : FunSpec({

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

    context("isActive") {

        test("withdrawalRequestedAt이 null이면 활성이다") {
            val member = MemberFixture.create()
            member.isActive() shouldBe true
        }

        test("withdrawalRequestedAt이 설정되면 비활성이다") {
            val member = MemberFixture.create()
            member.requestWithdrawal(LocalDateTime.now())
            member.isActive() shouldBe false
        }
    }

    context("isWithdrawalPending") {

        test("유예 기간 내면 true를 반환한다") {
            val member = MemberFixture.create()
            val requestedAt = LocalDateTime.of(2026, 5, 1, 10, 0)
            member.requestWithdrawal(requestedAt)

            member.isWithdrawalPending(requestedAt.plusDays(WithdrawalPolicy.GRACE_PERIOD_DAYS - 1)) shouldBe true
        }

        test("유예 기간을 지나면 false를 반환한다") {
            val member = MemberFixture.create()
            val requestedAt = LocalDateTime.of(2026, 5, 1, 10, 0)
            member.requestWithdrawal(requestedAt)

            member.isWithdrawalPending(requestedAt.plusDays(WithdrawalPolicy.GRACE_PERIOD_DAYS)) shouldBe false
        }

        test("신청한 적이 없으면 false를 반환한다") {
            val member = MemberFixture.create()
            member.isWithdrawalPending(LocalDateTime.now()) shouldBe false
        }
    }

    context("isWithdrawalExpired") {

        test("유예 기간을 지나면 true를 반환한다") {
            val member = MemberFixture.create()
            val requestedAt = LocalDateTime.of(2026, 5, 1, 10, 0)
            member.requestWithdrawal(requestedAt)

            member.isWithdrawalExpired(requestedAt.plusDays(WithdrawalPolicy.GRACE_PERIOD_DAYS)) shouldBe true
        }

        test("유예 기간 내면 false를 반환한다") {
            val member = MemberFixture.create()
            val requestedAt = LocalDateTime.of(2026, 5, 1, 10, 0)
            member.requestWithdrawal(requestedAt)

            member.isWithdrawalExpired(requestedAt.plusDays(1)) shouldBe false
        }

        test("신청한 적이 없으면 false를 반환한다") {
            val member = MemberFixture.create()
            member.isWithdrawalExpired(LocalDateTime.now()) shouldBe false
        }
    }

    context("withdrawalGraceWindowOrNull") {

        test("유예 기간 내면 requestedAt과 expiresAt을 가진 윈도우를 반환한다") {
            val member = MemberFixture.create()
            val requestedAt = LocalDateTime.of(2026, 5, 1, 10, 0)
            member.requestWithdrawal(requestedAt)

            val window = member.withdrawalGraceWindowOrNull(requestedAt.plusDays(1))

            window shouldNotBe null
            window!!.requestedAt shouldBe requestedAt
            window.expiresAt shouldBe requestedAt.plusDays(WithdrawalPolicy.GRACE_PERIOD_DAYS)
        }

        test("유예 기간을 지나면 null을 반환한다") {
            val member = MemberFixture.create()
            val requestedAt = LocalDateTime.of(2026, 5, 1, 10, 0)
            member.requestWithdrawal(requestedAt)

            member.withdrawalGraceWindowOrNull(requestedAt.plusDays(WithdrawalPolicy.GRACE_PERIOD_DAYS)) shouldBe null
        }

        test("신청한 적이 없으면 null을 반환한다") {
            val member = MemberFixture.create()
            member.withdrawalGraceWindowOrNull(LocalDateTime.now()) shouldBe null
        }
    }

    context("anonymize") {

        test("모든 PII를 sentinel 값으로 교체한 새 Member를 반환한다") {
            val member = MemberFixture.create(email = "user@example.com", nickname = "원래닉네임")
            member.requestWithdrawal(LocalDateTime.now())

            val anonymized = member.anonymize()

            anonymized.email shouldBe Email.withdrawn(member.id)
            anonymized.nickname shouldBe WithdrawnSentinel.nickname(member.id)
            anonymized.password shouldBe WithdrawnSentinel.PASSWORD
            anonymized.name shouldBe WithdrawnSentinel.NAME
            anonymized.phoneNumber.fullNumber shouldBe WithdrawnSentinel.PHONE_NUMBER
            anonymized.birthDate shouldBe WithdrawnSentinel.BIRTH_DATE
            anonymized.region shouldBe WithdrawnSentinel.REGION
            anonymized.highSchool shouldBe null
            anonymized.university shouldBe null
        }

        test("withdrawalRequestedAt은 보존된다") {
            val member = MemberFixture.create()
            val requestedAt = LocalDateTime.of(2026, 5, 1, 10, 0)
            member.requestWithdrawal(requestedAt)

            val anonymized = member.anonymize()

            anonymized.withdrawalRequestedAt shouldBe requestedAt
        }
    }
})
