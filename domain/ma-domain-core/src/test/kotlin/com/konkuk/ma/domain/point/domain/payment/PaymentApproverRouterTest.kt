package com.konkuk.ma.domain.point.domain.payment

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.exception.InvalidStateException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class PaymentApproverRouterTest : FunSpec({

    fun createRequest(method: PaymentMethod = PaymentMethod.CARD) = PaymentApprovalRequest(
        paymentMethod = method,
        paymentToken = "token-1",
        amount = Money.wons(1000),
        idempotencyKey = "idem-1",
        orderName = "테스트 상품",
    )

    fun createApproval(method: PaymentMethod = PaymentMethod.CARD) = PaymentApproval(
        approvalNumber = "MOCK-1",
        approvedAmount = Money.wons(1000),
        approvedAt = LocalDateTime.now(),
        paymentMethod = method,
    )

    context("approve") {

        test("주어진 결제 수단을 지원하는 첫 어댑터를 찾아 승인을 위임한다") {
            // Given
            val cardApprover = mockk<PaymentApprover>()
            val kakaoApprover = mockk<PaymentApprover>()
            val request = createRequest(PaymentMethod.CARD)
            val approval = createApproval(PaymentMethod.CARD)
            every { cardApprover.supports(PaymentMethod.CARD) } returns true
            every { kakaoApprover.supports(PaymentMethod.CARD) } returns false
            every { cardApprover.approve(request) } returns approval
            val router = PaymentApproverRouter(listOf(cardApprover, kakaoApprover))

            // When
            val result = router.approve(PaymentMethod.CARD, request)

            // Then
            result shouldBe approval
            verify { cardApprover.approve(request) }
            verify(exactly = 0) { kakaoApprover.approve(any()) }
        }

        test("여러 어댑터가 supports=true이면 리스트의 첫 어댑터가 승인한다") {
            // Given
            val firstApprover = mockk<PaymentApprover>()
            val secondApprover = mockk<PaymentApprover>()
            val request = createRequest(PaymentMethod.KAKAO_PAY)
            val approval = createApproval(PaymentMethod.KAKAO_PAY)
            every { firstApprover.supports(PaymentMethod.KAKAO_PAY) } returns true
            every { firstApprover.approve(request) } returns approval
            val router = PaymentApproverRouter(listOf(firstApprover, secondApprover))

            // When
            val result = router.approve(PaymentMethod.KAKAO_PAY, request)

            // Then
            result shouldBe approval
            verify(exactly = 0) { secondApprover.approve(any()) }
        }

        test("지원하는 어댑터가 없으면 InvalidStateException이 발생한다") {
            // Given
            val approver = mockk<PaymentApprover>()
            every { approver.supports(any()) } returns false
            val router = PaymentApproverRouter(listOf(approver))

            // When & Then
            shouldThrow<InvalidStateException> {
                router.approve(PaymentMethod.CARD, createRequest())
            }
        }

        test("어댑터 목록이 비어있으면 InvalidStateException이 발생한다") {
            val router = PaymentApproverRouter(emptyList())

            shouldThrow<InvalidStateException> {
                router.approve(PaymentMethod.CARD, createRequest())
            }
        }
    }
})
