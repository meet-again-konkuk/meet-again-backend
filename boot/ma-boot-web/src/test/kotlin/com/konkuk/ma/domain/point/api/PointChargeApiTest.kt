package com.konkuk.ma.domain.point.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.application.PointChargeService
import com.konkuk.ma.domain.point.application.result.ChargeResult
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import com.konkuk.ma.domain.point.exception.PaymentApprovalFailedException
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import com.konkuk.ma.exception.InvalidStateException
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.approvalNumber
import com.konkuk.ma.vocabulary.balance
import com.konkuk.ma.vocabulary.chargePointProductId
import com.konkuk.ma.vocabulary.chargedQuantity
import com.konkuk.ma.vocabulary.orderPointPrice
import com.konkuk.ma.vocabulary.idempotencyKey
import com.konkuk.ma.vocabulary.paidAmount
import com.konkuk.ma.vocabulary.paymentMethod
import com.konkuk.ma.vocabulary.paymentToken
import com.konkuk.ma.vocabulary.pointHistoryId
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(PointChargeApi::class)
@BaseApiTest
class PointChargeApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val pointChargeService: PointChargeService,
) : FunSpec({

    test("인연 충전 API 문서화 - 할인 없는 상품") {
        // Given
        val request = mapOf(
            "pointProductId" to 1L,
            "paymentMethod" to PaymentMethod.CARD.name,
            "paymentToken" to "pg_token_abc123",
            "orderPointPrice" to 2500,
            "idempotencyKey" to "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        )

        every { pointChargeService.charge(any()) } returns ChargeResult(
            pointHistoryId = 100L,
            balance = PointQuantity(30),
            chargedQuantity = PointQuantity(30),
            paidAmount = Money.wons(2500),
            approvalNumber = "MOCK-550e8400-e29b-41d4-a716-446655440000",
        )

        // When & Then
        mockMvc.postJson("/api/points") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isCreated() } }
            .andDocument(
                "point/charge",
                requestBody(
                    chargePointProductId(),
                    paymentMethod(),
                    paymentToken(),
                    orderPointPrice(),
                    idempotencyKey(),
                ),
                responseBody(
                    pointHistoryId(),
                    balance(),
                    chargedQuantity(),
                    paidAmount(),
                    approvalNumber(),
                ),
            )
    }

    test("인연 충전 API 문서화 - 할인 적용 상품") {
        // Given
        val request = mapOf(
            "pointProductId" to 1L,
            "paymentMethod" to PaymentMethod.KAKAO_PAY.name,
            "paymentToken" to "pg_token_kakao_xyz",
            "orderPointPrice" to 800,
            "idempotencyKey" to "11111111-2222-3333-4444-555555555555",
        )

        every { pointChargeService.charge(any()) } returns ChargeResult(
            pointHistoryId = 100L,
            balance = PointQuantity(10),
            chargedQuantity = PointQuantity(10),
            paidAmount = Money.wons(800),
            approvalNumber = "MOCK-1234abcd-5678-90ef-1234-567890abcdef",
        )

        // When & Then
        mockMvc.postJson("/api/points") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isCreated() } }
            .andDocument(
                "point/charge-discounted",
                requestBody(
                    chargePointProductId(),
                    paymentMethod(),
                    paymentToken(),
                    orderPointPrice() means "할인 적용된 최종 결제 예상 금액 (원)",
                    idempotencyKey(),
                ),
                responseBody(
                    pointHistoryId(),
                    balance(),
                    chargedQuantity(),
                    paidAmount() means "서버 재계산된 실제 결제 금액 (할인 적용)",
                    approvalNumber(),
                ),
            )
    }

    test("멱등키가 중복되면 409를 반환한다") {
        // Given
        val request = mapOf(
            "pointProductId" to 1L,
            "paymentMethod" to PaymentMethod.CARD.name,
            "paymentToken" to "pg_token_dup",
            "orderPointPrice" to 2500,
            "idempotencyKey" to "duplicated-idempotency-key",
        )

        every { pointChargeService.charge(any()) } throws DuplicateException(
            EntityType.POINT_HISTORY,
            "idempotencyKey",
            "duplicated-idempotency-key",
        )

        // When & Then
        mockMvc.postJson("/api/points") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isConflict() } }
            .andDocument("point/charge-duplicated-idempotency-key")
    }

    test("orderPointPrice와 서버 계산 금액이 다르면 400을 반환한다") {
        // Given
        val request = mapOf(
            "pointProductId" to 1L,
            "paymentMethod" to PaymentMethod.CARD.name,
            "paymentToken" to "pg_token_mismatch",
            "orderPointPrice" to 999,
            "idempotencyKey" to "price-mismatch-key",
        )

        every { pointChargeService.charge(any()) } throws InvalidStateException(
            PointChargeApiTest::class,
            999,
            "주문 가격(999원)과 상품 가격(1000원)이 다릅니다.",
        )

        // When & Then
        mockMvc.postJson("/api/points") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isBadRequest() } }
            .andDocument("point/charge-price-mismatch")
    }

    test("PointProduct가 존재하지 않으면 404를 반환한다") {
        // Given
        val request = mapOf(
            "pointProductId" to 1L,
            "paymentMethod" to PaymentMethod.CARD.name,
            "paymentToken" to "pg_token_notfound",
            "orderPointPrice" to 2500,
            "idempotencyKey" to "product-not-found-key",
        )

        every { pointChargeService.charge(any()) } throws EntityNotFoundException(
            EntityType.POINT_PRODUCT,
            "1",
        )

        // When & Then
        mockMvc.postJson("/api/points") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isNotFound() } }
            .andDocument("point/charge-product-not-found")
    }

    test("PG 결제 승인에 실패하면 402를 반환한다") {
        // Given
        val request = mapOf(
            "pointProductId" to 1L,
            "paymentMethod" to PaymentMethod.CARD.name,
            "paymentToken" to "FAIL_token_example",
            "orderPointPrice" to 2500,
            "idempotencyKey" to "payment-fail-key",
        )

        every { pointChargeService.charge(any()) } throws PaymentApprovalFailedException(
            paymentToken = "FAIL_token_example",
            reason = "Mock 결제 승인이 FAIL 토큰으로 거부되었습니다.",
        )

        // When & Then
        mockMvc.postJson("/api/points") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isPaymentRequired() } }
            .andDocument("point/charge-payment-approval-failed")
    }

    context("인연 충전 - 유효성 검증 실패") {
        val validRequest = mapOf(
            "pointProductId" to 1L,
            "paymentMethod" to PaymentMethod.CARD.name,
            "paymentToken" to "pg_token_abc123",
            "orderPointPrice" to 2500,
            "idempotencyKey" to "valid-idempotency-key",
        )

        test("paymentToken이 비어있으면 400을 반환한다") {
            val request = validRequest + ("paymentToken" to "")

            mockMvc.postJson("/api/points") {
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isBadRequest() } }
        }

        test("idempotencyKey가 비어있으면 400을 반환한다") {
            val request = validRequest + ("idempotencyKey" to "")

            mockMvc.postJson("/api/points") {
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isBadRequest() } }
        }

        test("orderPointPrice가 음수이면 400을 반환한다") {
            val request = validRequest + ("orderPointPrice" to -1)

            mockMvc.postJson("/api/points") {
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isBadRequest() } }
        }
    }
})
