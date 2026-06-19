package com.konkuk.ma.domain.point.application

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.application.command.ChargePointCommand
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

    fun createCommand(
        ownerId: Long = 1L,
        pointProductId: Long = 1L,
        paymentMethod: PaymentMethod = PaymentMethod.CARD,
        paymentToken: String = "token-1",
        orderPointPrice: Int = 1000,
        idempotencyKey: String = "idem-key-1",
    ) = ChargePointCommand(
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
            val command = createCommand(pointProductId = product.pointProductId, orderPointPrice = product.price.toInt())
            val existingMemberPoint = MemberPoint(
                id = 10L,
                ownerId = command.ownerId,
                balance = PointQuantity(5),
            )
            val approval = PaymentApproval(
                approvalNumber = "MOCK-1",
                approvedAmount = product.price,
                approvedAt = LocalDateTime.now(),
                paymentMethod = command.paymentMethod,
            )

            every { productFinder.findOne(product.pointProductId) } returns productWithDiscount
            every { paymentApproverRouter.approve(command.paymentMethod, any()) } returns approval
            every { memberPointRepository.findOneOrInitial(command.ownerId) } returns existingMemberPoint
            every { memberPointRepository.save(any()) } returns existingMemberPoint.id!!
            every { pointHistoryRepository.save(any()) } returns 100L

            // When
            val result = service.charge(command)

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
            savedHistorySlot.captured.ownerId shouldBe command.ownerId
            savedHistorySlot.captured.paidAmount shouldBe product.price
            savedHistorySlot.captured.approvalNumber shouldBe approval.approvalNumber
            savedHistorySlot.captured.paymentMethod shouldBe command.paymentMethod
            savedHistorySlot.captured.idempotencyKey shouldBe command.idempotencyKey
        }

        test("기존 MemberPoint가 없으면 초기 잔액에서 충전한 후 save 시 신규 insert 된다") {
            // Given
            val product = PointProductFixture.create(pointProductId = 1L, quantity = 10, price = 1000)
            val productWithDiscount = PointProductWithDiscount(product, null)
            val command = createCommand(pointProductId = product.pointProductId, orderPointPrice = product.price.toInt())
            val initial = MemberPoint.initial(command.ownerId)
            val approval = PaymentApproval(
                approvalNumber = "MOCK-NEW",
                approvedAmount = product.price,
                approvedAt = LocalDateTime.now(),
                paymentMethod = command.paymentMethod,
            )

            every { productFinder.findOne(product.pointProductId) } returns productWithDiscount
            every { paymentApproverRouter.approve(command.paymentMethod, any()) } returns approval
            every { memberPointRepository.findOneOrInitial(command.ownerId) } returns initial
            every { memberPointRepository.save(any()) } returns 11L
            every { pointHistoryRepository.save(any()) } returns 101L

            // When
            val result = service.charge(command)

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
            val command = createCommand(pointProductId = product.pointProductId, orderPointPrice = discountedPrice.toInt())
            val approval = PaymentApproval(
                approvalNumber = "MOCK-DISCOUNT",
                approvedAmount = discountedPrice,
                approvedAt = LocalDateTime.now(),
                paymentMethod = command.paymentMethod,
            )

            every { productFinder.findOne(product.pointProductId) } returns productWithDiscount
            every { paymentApproverRouter.approve(command.paymentMethod, any()) } returns approval
            every { memberPointRepository.findOneOrInitial(command.ownerId) } returns MemberPoint.initial(command.ownerId)
            every { memberPointRepository.save(any()) } returns 1L
            every { pointHistoryRepository.save(any()) } returns 1L

            // When
            val result = service.charge(command)

            // Then
            result.paidAmount shouldBe discountedPrice

            val paymentOrderSlot = slot<PaymentOrder>()
            verify { paymentApproverRouter.approve(command.paymentMethod, capture(paymentOrderSlot)) }
            paymentOrderSlot.captured.amount shouldBe discountedPrice

            val savedHistorySlot = slot<NewPointHistory>()
            verify { pointHistoryRepository.save(capture(savedHistorySlot)) }
            savedHistorySlot.captured.paidAmount shouldBe discountedPrice
        }

        test("검증 실패 시 PG 승인을 호출하지 않고 예외를 전파한다") {
            // Given
            val product = PointProductFixture.create(pointProductId = 1L, price = 1000)
            val productWithDiscount = PointProductWithDiscount(product, null)
            val command = createCommand(pointProductId = product.pointProductId, orderPointPrice = 500)

            every { productFinder.findOne(product.pointProductId) } returns productWithDiscount
            every { pointChargeValidator.validate(command.idempotencyKey, command.orderPointPrice, productWithDiscount) } throws
                com.konkuk.ma.exception.InvalidStateException(PointChargeService::class, 500, "가격 불일치")

            // When & Then
            shouldThrow<com.konkuk.ma.exception.InvalidStateException> {
                service.charge(command)
            }
            verify(exactly = 0) { paymentApproverRouter.approve(any(), any()) }
            verify(exactly = 0) { memberPointRepository.save(any()) }
            verify(exactly = 0) { pointHistoryRepository.save(any()) }
        }

        test("PG 승인이 실패하면 PaymentApprovalFailedException이 전파되고 잔액과 이력이 저장되지 않는다") {
            // Given
            val product = PointProductFixture.create(pointProductId = 1L, quantity = 10, price = 1000)
            val productWithDiscount = PointProductWithDiscount(product, null)
            val command = createCommand(
                pointProductId = product.pointProductId,
                orderPointPrice = product.price.toInt(),
                paymentToken = "FAIL-token",
            )

            every { productFinder.findOne(product.pointProductId) } returns productWithDiscount
            every { paymentApproverRouter.approve(command.paymentMethod, any()) } throws PaymentApprovalFailedException(
                paymentToken = command.paymentToken,
                reason = "Mock 결제 승인이 FAIL 토큰으로 거부되었습니다.",
            )

            // When & Then
            shouldThrow<PaymentApprovalFailedException> {
                service.charge(command)
            }
            verify(exactly = 0) { memberPointRepository.findOneOrInitial(any<Long>()) }
            verify(exactly = 0) { memberPointRepository.save(any()) }
            verify(exactly = 0) { pointHistoryRepository.save(any()) }
        }
    }
})
