package com.konkuk.ma.domain.point.domain.payment

import com.konkuk.ma.exception.InvalidStateException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class PaymentApproverRouterTest : FunSpec({

    context("resolve") {

        test("주어진 결제 수단을 지원하는 첫 어댑터를 반환한다") {
            // Given
            val cardApprover = mockk<PaymentApprover>()
            val kakaoApprover = mockk<PaymentApprover>()
            every { cardApprover.supports(PaymentMethod.CARD) } returns true
            every { kakaoApprover.supports(PaymentMethod.CARD) } returns false
            val router = PaymentApproverRouter(listOf(cardApprover, kakaoApprover))

            // When
            val resolved = router.resolve(PaymentMethod.CARD)

            // Then
            resolved shouldBe cardApprover
        }

        test("여러 어댑터가 supports=true이면 리스트의 첫 어댑터를 반환한다") {
            // Given
            val firstApprover = mockk<PaymentApprover>()
            val secondApprover = mockk<PaymentApprover>()
            every { firstApprover.supports(PaymentMethod.KAKAO_PAY) } returns true
            val router = PaymentApproverRouter(listOf(firstApprover, secondApprover))

            // When
            val resolved = router.resolve(PaymentMethod.KAKAO_PAY)

            // Then
            resolved shouldBe firstApprover
        }

        test("지원하는 어댑터가 없으면 InvalidStateException이 발생한다") {
            // Given
            val approver = mockk<PaymentApprover>()
            every { approver.supports(any()) } returns false
            val router = PaymentApproverRouter(listOf(approver))

            // When & Then
            shouldThrow<InvalidStateException> {
                router.resolve(PaymentMethod.CARD)
            }
        }

        test("어댑터 목록이 비어있으면 InvalidStateException이 발생한다") {
            val router = PaymentApproverRouter(emptyList())

            shouldThrow<InvalidStateException> {
                router.resolve(PaymentMethod.CARD)
            }
        }
    }
})
