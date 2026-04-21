package com.konkuk.ma.domain.point.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.application.command.ChargePointCommand
import com.konkuk.ma.domain.point.domain.PointChargeValidator
import com.konkuk.ma.domain.point.domain.balance.MemberPoint
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.payment.PaymentApproval
import com.konkuk.ma.domain.point.domain.payment.PaymentApprovalRequest
import com.konkuk.ma.domain.point.domain.payment.PaymentApprover
import com.konkuk.ma.domain.point.domain.payment.PaymentApproverRouter
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import com.konkuk.ma.domain.point.domain.port.DiscountPolicyQueryRepository
import com.konkuk.ma.domain.point.domain.port.MemberPointRepository
import com.konkuk.ma.domain.point.domain.port.PointProductQueryRepository
import com.konkuk.ma.domain.point.domain.port.PointTransactionRepository
import com.konkuk.ma.domain.point.domain.transaction.NewPointTransaction
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

    val pointProductQueryRepository = mockk<PointProductQueryRepository>()
    val discountPolicyQueryRepository = mockk<DiscountPolicyQueryRepository>()
    val memberPointRepository = mockk<MemberPointRepository>()
    val pointTransactionRepository = mockk<PointTransactionRepository>()
    val paymentApproverRouter = mockk<PaymentApproverRouter>()
    val pointChargeValidator = mockk<PointChargeValidator>(relaxUnitFun = true)
    val approver = mockk<PaymentApprover>()
    val service = PointChargeService(
        pointProductQueryRepository,
        discountPolicyQueryRepository,
        memberPointRepository,
        pointTransactionRepository,
        paymentApproverRouter,
        pointChargeValidator,
    )

    beforeEach {
        clearAllMocks()
        every { pointChargeValidator.validate(any(), any()) } returns Unit
    }

    fun createCommand(
        email: String = "holeman@naver.com",
        pointProductId: Long = 1L,
        paymentMethod: PaymentMethod = PaymentMethod.CARD,
        paymentToken: String = "token-1",
        expectedPrice: Int = 1000,
        idempotencyKey: String = "idem-key-1",
    ) = ChargePointCommand(
        email = email,
        pointProductId = pointProductId,
        paymentMethod = paymentMethod,
        paymentToken = paymentToken,
        expectedPrice = expectedPrice,
        idempotencyKey = idempotencyKey,
    )

    context("charge") {

        test("할인이 없는 정상 흐름에서 잔액을 충전하고 거래 이력을 저장한 뒤 결과를 반환한다") {
            // Given
            val product = PointProductFixture.create(pointProductId = 1L, quantity = 30, price = 2000)
            val command = createCommand(pointProductId = product.pointProductId, expectedPrice = product.price.toInt())
            val existingMemberPoint = MemberPoint(
                id = 10L,
                ownerEmail = command.ownerEmail,
                balance = PointQuantity(5),
            )
            val approval = PaymentApproval(
                approvalNumber = "MOCK-1",
                approvedAmount = product.price,
                approvedAt = LocalDateTime.now(),
                paymentMethod = command.paymentMethod,
            )

            every { pointProductQueryRepository.findOne(product.pointProductId) } returns product
            every { paymentApproverRouter.resolve(command.paymentMethod) } returns approver
            every { approver.approve(any()) } returns approval
            every { memberPointRepository.findOneOrInitial(command.ownerEmail) } returns existingMemberPoint
            every { memberPointRepository.save(any()) } returns existingMemberPoint.id!!
            every { pointTransactionRepository.save(any()) } returns 100L

            // When
            val result = service.charge(command)

            // Then
            result.pointTransactionId shouldBe 100L
            result.balance shouldBe PointQuantity(existingMemberPoint.balance.toInt() + product.quantity.toInt())
            result.chargedQuantity shouldBe product.quantity
            result.paidAmount shouldBe product.price
            result.approvalNumber shouldBe approval.approvalNumber

            val savedMemberPointSlot = slot<MemberPoint>()
            verify { memberPointRepository.save(capture(savedMemberPointSlot)) }
            savedMemberPointSlot.captured.balance shouldBe result.balance

            val savedTransactionSlot = slot<NewPointTransaction>()
            verify { pointTransactionRepository.save(capture(savedTransactionSlot)) }
            savedTransactionSlot.captured.ownerEmail shouldBe command.ownerEmail
            savedTransactionSlot.captured.paidAmount shouldBe product.price
            savedTransactionSlot.captured.approvalNumber shouldBe approval.approvalNumber
            savedTransactionSlot.captured.paymentMethod shouldBe command.paymentMethod
            savedTransactionSlot.captured.idempotencyKey shouldBe command.idempotencyKey
        }

        test("기존 MemberPoint가 없으면 초기 잔액에서 충전한 후 save 시 신규 insert 된다") {
            // Given
            val product = PointProductFixture.create(pointProductId = 1L, quantity = 10, price = 1000)
            val command = createCommand(pointProductId = product.pointProductId, expectedPrice = product.price.toInt())
            val initial = MemberPoint.initial(command.ownerEmail)
            val approval = PaymentApproval(
                approvalNumber = "MOCK-NEW",
                approvedAmount = product.price,
                approvedAt = LocalDateTime.now(),
                paymentMethod = command.paymentMethod,
            )

            every { pointProductQueryRepository.findOne(product.pointProductId) } returns product
            every { paymentApproverRouter.resolve(command.paymentMethod) } returns approver
            every { approver.approve(any()) } returns approval
            every { memberPointRepository.findOneOrInitial(command.ownerEmail) } returns initial
            every { memberPointRepository.save(any()) } returns 11L
            every { pointTransactionRepository.save(any()) } returns 101L

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
            val discountedPrice = Money.wons(product.price.toInt() - 300)
            val command = createCommand(pointProductId = product.pointProductId, expectedPrice = discountedPrice.toInt())
            val approval = PaymentApproval(
                approvalNumber = "MOCK-DISCOUNT",
                approvedAmount = discountedPrice,
                approvedAt = LocalDateTime.now(),
                paymentMethod = command.paymentMethod,
            )

            every { pointProductQueryRepository.findOne(product.pointProductId) } returns product
            every { discountPolicyQueryRepository.findOneOrNull(discountPolicy.discountPolicyId) } returns discountPolicy
            every { paymentApproverRouter.resolve(command.paymentMethod) } returns approver
            every { approver.approve(any()) } returns approval
            every { memberPointRepository.findOneOrInitial(command.ownerEmail) } returns MemberPoint.initial(command.ownerEmail)
            every { memberPointRepository.save(any()) } returns 1L
            every { pointTransactionRepository.save(any()) } returns 1L

            // When
            val result = service.charge(command)

            // Then
            result.paidAmount shouldBe discountedPrice

            val approvalRequestSlot = slot<PaymentApprovalRequest>()
            verify { approver.approve(capture(approvalRequestSlot)) }
            approvalRequestSlot.captured.amount shouldBe discountedPrice

            val savedTransactionSlot = slot<NewPointTransaction>()
            verify { pointTransactionRepository.save(capture(savedTransactionSlot)) }
            savedTransactionSlot.captured.paidAmount shouldBe discountedPrice
        }

        test("검증 실패 시 PG 승인을 호출하지 않고 예외를 전파한다") {
            // Given
            val product = PointProductFixture.create(pointProductId = 1L, price = 1000)
            val command = createCommand(pointProductId = product.pointProductId, expectedPrice = 500)

            every { pointProductQueryRepository.findOne(product.pointProductId) } returns product
            every { pointChargeValidator.validate(command, product.price) } throws
                com.konkuk.ma.exception.InvalidStateException(PointChargeService::class, 500, "가격 불일치")

            // When & Then
            shouldThrow<com.konkuk.ma.exception.InvalidStateException> {
                service.charge(command)
            }
            verify(exactly = 0) { paymentApproverRouter.resolve(any()) }
            verify(exactly = 0) { memberPointRepository.save(any()) }
            verify(exactly = 0) { pointTransactionRepository.save(any()) }
        }

        test("PG 승인이 실패하면 PaymentApprovalFailedException이 전파되고 잔액과 이력이 저장되지 않는다") {
            // Given
            val product = PointProductFixture.create(pointProductId = 1L, quantity = 10, price = 1000)
            val command = createCommand(
                pointProductId = product.pointProductId,
                expectedPrice = product.price.toInt(),
                paymentToken = "FAIL-token",
            )

            every { pointProductQueryRepository.findOne(product.pointProductId) } returns product
            every { paymentApproverRouter.resolve(command.paymentMethod) } returns approver
            every { approver.approve(any()) } throws PaymentApprovalFailedException(
                paymentToken = command.paymentToken,
                reason = "Mock 결제 승인이 FAIL 토큰으로 거부되었습니다.",
            )

            // When & Then
            shouldThrow<PaymentApprovalFailedException> {
                service.charge(command)
            }
            verify(exactly = 0) { memberPointRepository.findOneOrInitial(any<Email>()) }
            verify(exactly = 0) { memberPointRepository.save(any()) }
            verify(exactly = 0) { pointTransactionRepository.save(any()) }
        }
    }
})
