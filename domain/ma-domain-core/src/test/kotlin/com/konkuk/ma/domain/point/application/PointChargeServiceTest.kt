package com.konkuk.ma.domain.point.application

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.PointChargeValidator
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.PointProductWithDiscountFinder
import com.konkuk.ma.domain.point.domain.balance.MemberPoint
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.payment.PaymentApproval
import com.konkuk.ma.domain.point.domain.payment.PaymentOrder
import com.konkuk.ma.domain.point.domain.payment.PaymentApproverRouter
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import com.konkuk.ma.domain.point.domain.history.NewPointHistory
import com.konkuk.ma.domain.point.domain.port.MemberPointRepository
import com.konkuk.ma.domain.point.domain.port.PointHistoryRepository
import com.konkuk.ma.domain.point.exception.PaymentApprovalFailedException
import com.konkuk.ma.domain.point.fixture.DiscountPolicyFixture
import com.konkuk.ma.domain.point.fixture.PointProductFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime

class PointChargeServiceTest : FunSpec({

    val productFinder = mockk<PointProductWithDiscountFinder>()
    val pointChargeValidator = mockk<PointChargeValidator>(relaxUnitFun = true)
    val paymentApproverRouter = mockk<PaymentApproverRouter>()
    val memberPointRepository = mockk<MemberPointRepository>()
    val pointHistoryRepository = mockk<PointHistoryRepository>()
    val service = PointChargeService(
        productFinder,
        pointChargeValidator,
        paymentApproverRouter,
        memberPointRepository,
        pointHistoryRepository,
    )

    beforeEach {
        clearAllMocks()
        every { pointChargeValidator.validate(any(), any(), any()) } returns Unit
    }

    val ownerId = 1L
    val paymentMethod = PaymentMethod.CARD
    val idempotencyKey = "idem-key-1"

    fun charge(
        pointProductId: Long,
        orderPointPrice: Int,
        paymentToken: String = "token-1",
    ) = service.charge(
        ownerId = ownerId,
        pointProductId = pointProductId,
        paymentMethod = paymentMethod,
        paymentToken = paymentToken,
        orderPointPrice = orderPointPrice,
        idempotencyKey = idempotencyKey,
    )

    context("charge") {

        test("할인이 없는 정상 흐름에서 잔액을 충전하고 거래 이력을 저장한 뒤 결과를 반환한다") {
            // Given
            val product = PointProductFixture.create(pointProductId = 1L, quantity = 30, price = 2000)
            val productWithDiscount = PointProductWithDiscount(product, null)
            val existingMemberPoint = MemberPoint(
                id = 10L,
                ownerId = ownerId,
                balance = PointQuantity(5),
            )
            val approval = PaymentApproval(
                approvalNumber = "MOCK-1",
                approvedAmount = product.price,
                approvedAt = LocalDateTime.now(),
                paymentMethod = paymentMethod,
            )

            every { productFinder.findOne(product.pointProductId) } returns productWithDiscount
            every { paymentApproverRouter.approve(paymentMethod, any()) } returns approval
            every { memberPointRepository.findOneOrInitial(ownerId) } returns existingMemberPoint
            every { memberPointRepository.save(any()) } returns existingMemberPoint.id!!
            every { pointHistoryRepository.save(any()) } returns 100L

            // When
            val result = charge(product.pointProductId, product.price.toInt())

            // Then
            result.pointHistoryId shouldBe 100L
            result.balance shouldBe PointQuantity(existingMemberPoint.balance.toInt() + product.quantity.toInt())
            result.chargedQuantity shouldBe product.quantity
            result.paidAmount shouldBe product.price
            result.approvalNumber shouldBe approval.approvalNumber

            val savedMemberPointSlot = slot<MemberPoint>()
            verify { memberPointRepository.save(capture(savedMemberPointSlot)) }
            savedMemberPointSlot.captured.balance shouldBe result.balance

            val savedHistorySlot = slot<NewPointHistory>()
            verify { pointHistoryRepository.save(capture(savedHistorySlot)) }
            savedHistorySlot.captured.ownerId shouldBe ownerId
            savedHistorySlot.captured.paidAmount shouldBe product.price
            savedHistorySlot.captured.approvalNumber shouldBe approval.approvalNumber
            savedHistorySlot.captured.paymentMethod shouldBe paymentMethod
            savedHistorySlot.captured.idempotencyKey shouldBe idempotencyKey
        }

        test("기존 MemberPoint가 없으면 초기 잔액에서 충전한 후 save 시 신규 insert 된다") {
            // Given
            val product = PointProductFixture.create(pointProductId = 1L, quantity = 10, price = 1000)
            val productWithDiscount = PointProductWithDiscount(product, null)
            val initial = MemberPoint.initial(ownerId)
            val approval = PaymentApproval(
                approvalNumber = "MOCK-NEW",
                approvedAmount = product.price,
                approvedAt = LocalDateTime.now(),
                paymentMethod = paymentMethod,
            )

            every { productFinder.findOne(product.pointProductId) } returns productWithDiscount
            every { paymentApproverRouter.approve(paymentMethod, any()) } returns approval
            every { memberPointRepository.findOneOrInitial(ownerId) } returns initial
            every { memberPointRepository.save(any()) } returns 11L
            every { pointHistoryRepository.save(any()) } returns 101L

            // When
            val result = charge(product.pointProductId, product.price.toInt())

            // Then
            result.balance shouldBe product.quantity
            result.chargedQuantity shouldBe product.quantity
        }

        test("할인 정책이 적용된 경우 할인 가격으로 PG 승인 요청과 이력 기록이 수행된다") {
            // Given
            val discountPolicy = DiscountPolicyFixture.createAmount(discountPolicyId = 10L, discountAmount = 300)
            val product = PointProductFixture.create(
                pointProductId = 2L,
                quantity = 20,
                price = 1000,
                discountPolicyId = discountPolicy.discountPolicyId,
            )
            val productWithDiscount = PointProductWithDiscount(product, discountPolicy)
            val discountedPrice = Money.wons(product.price.toInt() - 300)
            val approval = PaymentApproval(
                approvalNumber = "MOCK-DISCOUNT",
                approvedAmount = discountedPrice,
                approvedAt = LocalDateTime.now(),
                paymentMethod = paymentMethod,
            )

            every { productFinder.findOne(product.pointProductId) } returns productWithDiscount
            every { paymentApproverRouter.approve(paymentMethod, any()) } returns approval
            every { memberPointRepository.findOneOrInitial(ownerId) } returns MemberPoint.initial(ownerId)
            every { memberPointRepository.save(any()) } returns 1L
            every { pointHistoryRepository.save(any()) } returns 1L

            // When
            val result = charge(product.pointProductId, discountedPrice.toInt())

            // Then
            result.paidAmount shouldBe discountedPrice

            val paymentOrderSlot = slot<PaymentOrder>()
            verify { paymentApproverRouter.approve(paymentMethod, capture(paymentOrderSlot)) }
            paymentOrderSlot.captured.amount shouldBe discountedPrice

            val savedHistorySlot = slot<NewPointHistory>()
            verify { pointHistoryRepository.save(capture(savedHistorySlot)) }
            savedHistorySlot.captured.paidAmount shouldBe discountedPrice
        }

        test("검증 실패 시 PG 승인을 호출하지 않고 예외를 전파한다") {
            // Given
            val product = PointProductFixture.create(pointProductId = 1L, price = 1000)
            val productWithDiscount = PointProductWithDiscount(product, null)
            val orderPointPrice = 500

            every { productFinder.findOne(product.pointProductId) } returns productWithDiscount
            every { pointChargeValidator.validate(idempotencyKey, orderPointPrice, productWithDiscount) } throws
                com.konkuk.ma.exception.InvalidStateException(PointChargeService::class, 500, "가격 불일치")

            // When & Then
            shouldThrow<com.konkuk.ma.exception.InvalidStateException> {
                charge(product.pointProductId, orderPointPrice)
            }
            verify(exactly = 0) { paymentApproverRouter.approve(any(), any()) }
            verify(exactly = 0) { memberPointRepository.save(any()) }
            verify(exactly = 0) { pointHistoryRepository.save(any()) }
        }

        test("PG 승인이 실패하면 PaymentApprovalFailedException이 전파되고 잔액과 이력이 저장되지 않는다") {
            // Given
            val product = PointProductFixture.create(pointProductId = 1L, quantity = 10, price = 1000)
            val productWithDiscount = PointProductWithDiscount(product, null)
            val failPaymentToken = "FAIL-token"

            every { productFinder.findOne(product.pointProductId) } returns productWithDiscount
            every { paymentApproverRouter.approve(paymentMethod, any()) } throws PaymentApprovalFailedException(
                paymentToken = failPaymentToken,
                reason = "Mock 결제 승인이 FAIL 토큰으로 거부되었습니다.",
            )

            // When & Then
            shouldThrow<PaymentApprovalFailedException> {
                charge(product.pointProductId, product.price.toInt(), paymentToken = failPaymentToken)
            }
            verify(exactly = 0) { memberPointRepository.findOneOrInitial(any<Long>()) }
            verify(exactly = 0) { memberPointRepository.save(any()) }
            verify(exactly = 0) { pointHistoryRepository.save(any()) }
        }
    }
})
