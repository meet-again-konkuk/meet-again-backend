package com.konkuk.ma.domain.point.domain.balance

import com.konkuk.ma.exception.InvalidStateException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class MemberPointTest : FunSpec({

    val ownerId = 1L

    context("initial") {

        test("잔액이 0이고 id가 null인 초기 MemberPoint를 생성한다") {
            val memberPoint = MemberPoint.initial(ownerId)

            memberPoint.id shouldBe null
            memberPoint.ownerId shouldBe ownerId
            memberPoint.balance shouldBe PointQuantity.ZERO
            memberPoint.isPersisted().shouldBeFalse()
        }
    }

    context("isPersisted") {

        test("id가 null이면 false를 반환한다") {
            MemberPoint.initial(ownerId).isPersisted().shouldBeFalse()
        }

        test("id가 있으면 true를 반환한다") {
            val persisted = MemberPoint(id = 1L, ownerId = ownerId, balance = PointQuantity.ZERO)

            persisted.isPersisted().shouldBeTrue()
        }
    }

    context("charge") {

        test("현재 잔액에 충전 수량을 더한 MemberPoint를 반환한다") {
            val memberPoint = MemberPoint(id = 1L, ownerId = ownerId, balance = PointQuantity(10))

            val charged = memberPoint.charge(PointQuantity(5))

            charged.balance shouldBe PointQuantity(15)
            charged.id shouldBe memberPoint.id
            charged.ownerId shouldBe memberPoint.ownerId
        }

        test("초기 MemberPoint에서 충전하면 id가 null로 유지된다") {
            val memberPoint = MemberPoint.initial(ownerId)

            val charged = memberPoint.charge(PointQuantity(30))

            charged.id shouldBe null
            charged.balance shouldBe PointQuantity(30)
        }
    }

    context("spend") {

        test("현재 잔액에서 사용 수량을 뺀 MemberPoint를 반환한다") {
            val memberPoint = MemberPoint(id = 1L, ownerId = ownerId, balance = PointQuantity(10))

            val spent = memberPoint.spend(PointQuantity(3))

            spent.balance shouldBe PointQuantity(7)
            spent.id shouldBe memberPoint.id
            spent.ownerId shouldBe memberPoint.ownerId
        }

        test("잔액이 부족하면 InvalidStateException이 발생한다") {
            val memberPoint = MemberPoint(id = 1L, ownerId = ownerId, balance = PointQuantity(3))

            shouldThrow<InvalidStateException> {
                memberPoint.spend(PointQuantity(10))
            }
        }
    }
})
