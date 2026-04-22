package com.konkuk.ma.config

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import com.konkuk.ma.domain.point.domain.payment.PaymentOrder
import com.konkuk.ma.domain.point.exception.PaymentApprovalFailedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

class MockPaymentApproverTest : FunSpec({

    val mockPaymentApprover = MockPaymentApprover()

    fun createOrder(
        paymentMethod: PaymentMethod = PaymentMethod.CARD,
        paymentToken: String = "valid-token",
        amount: Money = Money.wons(1000),
        idempotencyKey: String = "idem-key",
        orderName: String = "인연 10개",
    ): PaymentOrder = PaymentOrder(
        paymentMethod = paymentMethod,
        paymentToken = paymentToken,
        amount = amount,
        idempotencyKey = idempotencyKey,
        orderName = orderName,
    )

    context("supports") {

        test("모든 결제 수단에 대해 true를 반환한다") {
            PaymentMethod.entries.forEach { method ->
                mockPaymentApprover.supports(method).shouldBeTrue()
            }
        }
    }

    context("approve") {

        test("정상 토큰이면 MOCK- 접두어를 가진 PaymentApproval을 반환한다") {
            // Given
            val order = createOrder(paymentToken = "valid-token-1")

            // When
            val approval = mockPaymentApprover.approve(order)

            // Then
            approval.approvalNumber shouldStartWith "MOCK-"
            approval.approvedAmount shouldBe order.amount
            approval.paymentMethod shouldBe order.paymentMethod
        }

        test("요청마다 서로 다른 approvalNumber를 발급한다") {
            // Given
            val order = createOrder()

            // When
            val first = mockPaymentApprover.approve(order)
            val second = mockPaymentApprover.approve(order)

            // Then
            (first.approvalNumber == second.approvalNumber) shouldBe false
        }

        test("FAIL로 시작하는 토큰이면 PaymentApprovalFailedException이 발생한다") {
            // Given
            val failOrder = createOrder(paymentToken = "FAIL-token-1")

            // When & Then
            shouldThrow<PaymentApprovalFailedException> {
                mockPaymentApprover.approve(failOrder)
            }
        }

        test("FAIL 토큰은 대소문자 구분되며 소문자 fail은 정상 승인된다") {
            // Given
            val order = createOrder(paymentToken = "fail-token")

            // When
            val approval = mockPaymentApprover.approve(order)

            // Then
            approval.approvalNumber shouldStartWith "MOCK-"
        }
    }

    context("cancel") {

        test("예외 없이 no-op으로 동작한다") {
            mockPaymentApprover.cancel(approvalNumber = "MOCK-UUID", reason = "테스트 취소")
        }
    }
})
