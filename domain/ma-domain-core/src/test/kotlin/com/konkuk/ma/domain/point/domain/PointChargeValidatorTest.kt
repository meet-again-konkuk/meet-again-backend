package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.application.command.ChargePointCommand
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.history.PointHistory
import com.konkuk.ma.domain.point.domain.history.PointHistoryType
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import com.konkuk.ma.domain.point.domain.port.PointHistoryRepository
import com.konkuk.ma.domain.point.fixture.PointProductFixture
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.InvalidStateException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class PointChargeValidatorTest : FunSpec({

    val pointHistoryRepository = mockk<PointHistoryRepository>()
    val pointChargeValidator = PointChargeValidator(pointHistoryRepository)

    fun createCommand(
        idempotencyKey: String = "idem-key-1",
        orderPointPrice: Int = 1000,
    ): ChargePointCommand {
        return ChargePointCommand(
            email = "holeman@naver.com",
            pointProductId = 1L,
            paymentMethod = PaymentMethod.CARD,
            paymentToken = "token-1",
            orderPointPrice = orderPointPrice,
            idempotencyKey = idempotencyKey,
        )
    }

    fun createProduct(price: Int = 1000): PointProductWithDiscount {
        return PointProductWithDiscount(
            pointProduct = PointProductFixture.create(price = price),
            discountPolicy = null,
        )
    }

    fun createExistingHistory(idempotencyKey: String): PointHistory {
        return PointHistory(
            id = 100L,
            ownerEmail = Email("holeman@naver.com"),
            pointProductId = 1L,
            historyType = PointHistoryType.CHARGE,
            quantity = PointQuantity(10),
            paidAmount = Money.wons(1000),
            paymentMethod = PaymentMethod.CARD,
            idempotencyKey = idempotencyKey,
            approvalNumber = "MOCK-PREV",
            createdDate = LocalDateTime.now(),
        )
    }

    context("validate") {

        test("멱등키가 존재하지 않고 가격이 일치하면 예외 없이 통과한다") {
            // Given
            val command = createCommand(orderPointPrice = 1000)
            val product = createProduct(price = 1000)
            every { pointHistoryRepository.findOneOrNull(command.idempotencyKey) } returns null

            // When
            pointChargeValidator.validate(command.idempotencyKey, command.orderPointPrice, product)
        }

        test("멱등키가 이미 존재하면 DuplicateException이 발생한다") {
            // Given
            val command = createCommand(idempotencyKey = "duplicate-key")
            val product = createProduct(price = 1000)
            every {
                pointHistoryRepository.findOneOrNull(command.idempotencyKey)
            } returns createExistingHistory(command.idempotencyKey)

            // When & Then
            shouldThrow<DuplicateException> {
                pointChargeValidator.validate(command.idempotencyKey, command.orderPointPrice, product)
            }
        }

        test("주문 가격이 상품 가격과 다르면 InvalidStateException이 발생한다") {
            // Given
            val command = createCommand(orderPointPrice = 1000)
            val product = createProduct(price = 2000)
            every { pointHistoryRepository.findOneOrNull(command.idempotencyKey) } returns null

            // When & Then
            shouldThrow<InvalidStateException> {
                pointChargeValidator.validate(command.idempotencyKey, command.orderPointPrice, product)
            }
        }

        test("멱등키 중복이 먼저 검증되어 가격 불일치보다 우선 예외를 발생시킨다") {
            // Given: 멱등키 중복 AND 가격 불일치를 모두 만족하는 상황
            val command = createCommand(idempotencyKey = "dup-key", orderPointPrice = 500)
            val product = createProduct(price = 1000)
            every {
                pointHistoryRepository.findOneOrNull(command.idempotencyKey)
            } returns createExistingHistory(command.idempotencyKey)

            // When & Then: DuplicateException이 먼저 발생
            shouldThrow<DuplicateException> {
                pointChargeValidator.validate(command.idempotencyKey, command.orderPointPrice, product)
            }
        }
    }
})
