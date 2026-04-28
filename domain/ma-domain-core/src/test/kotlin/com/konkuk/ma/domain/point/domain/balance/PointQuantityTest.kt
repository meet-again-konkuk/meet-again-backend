package com.konkuk.ma.domain.point.domain.balance

import com.konkuk.ma.exception.InvalidStateException
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class PointQuantityTest : FunSpec({

    context("PointQuantity 객체 생성 테스트") {

        test("0 이상의 값으로 정상 생성된다") {
            val quantity = PointQuantity(10)

            quantity.value shouldBe 10
            quantity.toInt() shouldBe 10
        }

        test("0으로 생성해도 정상 생성된다") {
            val quantity = PointQuantity(0)

            quantity.value shouldBe 0
            quantity.isZero().shouldBeTrue()
        }

        test("음수로 생성하면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                PointQuantity(-1)
            }
        }
    }

    context("plus") {

        test("두 수량을 더한 결과를 반환한다") {
            val first = PointQuantity(10)
            val second = PointQuantity(5)

            val result = first + second

            result shouldBe PointQuantity(15)
        }

        test("0을 더해도 값이 유지된다") {
            val quantity = PointQuantity(10)

            (quantity + PointQuantity.ZERO) shouldBe quantity
        }
    }

    context("minus") {

        test("부족하지 않은 경우 두 수량의 차이를 반환한다") {
            val balance = PointQuantity(10)
            val spend = PointQuantity(3)

            val result = balance - spend

            result shouldBe PointQuantity(7)
        }

        test("동일한 수량을 빼면 0이 된다") {
            val quantity = PointQuantity(10)

            (quantity - quantity).isZero().shouldBeTrue()
        }

        test("잔액이 부족하면 InvalidStateException이 발생한다") {
            val balance = PointQuantity(3)
            val spend = PointQuantity(10)

            shouldThrow<InvalidStateException> {
                balance - spend
            }
        }
    }

    context("isLessThan") {

        test("자신이 다른 값보다 작으면 true를 반환한다") {
            PointQuantity(3).isLessThan(PointQuantity(5)).shouldBeTrue()
        }

        test("자신이 다른 값보다 크면 false를 반환한다") {
            PointQuantity(5).isLessThan(PointQuantity(3)).shouldBeFalse()
        }

        test("같은 값이면 false를 반환한다") {
            PointQuantity(3).isLessThan(PointQuantity(3)).shouldBeFalse()
        }
    }

    context("isZero") {

        test("값이 0이면 true를 반환한다") {
            PointQuantity(0).isZero().shouldBeTrue()
        }

        test("값이 0이 아니면 false를 반환한다") {
            PointQuantity(1).isZero().shouldBeFalse()
        }
    }

    context("ZERO 상수") {

        test("ZERO는 값이 0인 PointQuantity이다") {
            PointQuantity.ZERO.value shouldBe 0
            PointQuantity.ZERO.isZero().shouldBeTrue()
        }
    }

    context("toInt") {

        test("내부 값을 Int로 반환한다") {
            PointQuantity(42).toInt() shouldBe 42
        }
    }

    context("동등성") {

        test("값이 같으면 동일한 객체로 취급한다") {
            PointQuantity(10) shouldBe PointQuantity(10)
        }

        test("값이 다르면 다른 객체로 취급한다") {
            (PointQuantity(10) == PointQuantity(11)).shouldBeFalse()
        }
    }
})
