package com.konkuk.ma.vocabulary

import com.konkuk.ma.extension.BOOLEAN
import com.konkuk.ma.extension.ENUM
import com.konkuk.ma.extension.NUMBER
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.responseType
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod

// --- 포인트 상품 관련 필드 ---

fun pointProductId(fieldName: String = "pointProductId") =
    fieldName responseType NUMBER means "포인트 상품 ID" example "1"

fun pointProductName(fieldName: String = "name") =
    fieldName responseType STRING means "상품명" example "인연 10개"

fun quantity(fieldName: String = "quantity") =
    fieldName responseType NUMBER means "인연 수량" example "10"

fun price(fieldName: String = "price") =
    fieldName responseType NUMBER means "가격 (원)" example "1000"

fun discountedPrice(fieldName: String = "discountedPrice") =
    fieldName responseType NUMBER means "최종 가격 (원, 할인 비활성 시 원가)" example "800"

fun discountRate(fieldName: String = "discountRate") =
    fieldName responseType NUMBER means "할인율 (%, 정수 반올림, 할인 비활성 시 0)" example "20"

fun discountType(fieldName: String = "discountType") =
    fieldName responseType STRING means "할인 유형 (AMOUNT, PERCENT, 없으면 null)" example "AMOUNT"

fun isDiscountActive(fieldName: String = "isDiscountActive") =
    fieldName responseType BOOLEAN means "할인 활성 여부" example "true"

// --- 인연 충전 요청 필드 ---

fun chargePointProductId(fieldName: String = "pointProductId") =
    fieldName responseType STRING means "구매할 포인트 상품 ID (암호화)" example "AbCdEfGh"

fun paymentMethod(fieldName: String = "paymentMethod") =
    fieldName responseType ENUM(PaymentMethod::class) means "결제수단"

fun paymentToken(fieldName: String = "paymentToken") =
    fieldName responseType STRING means "PG사 결제 인증 토큰" example "pg_token_abc123"

fun expectedPrice(fieldName: String = "expectedPrice") =
    fieldName responseType NUMBER means "클라이언트가 화면에서 본 최종 결제 예상 금액 (원). 서버 재계산값과 불일치 시 실패" example "1000"

fun idempotencyKey(fieldName: String = "idempotencyKey") =
    fieldName responseType STRING means "멱등키 (클라이언트 생성 UUID, 동일 키 재요청은 409)" example "a1b2c3d4-e5f6-7890-abcd-ef1234567890"

// --- 인연 충전 응답 필드 ---

fun pointTransactionId(fieldName: String = "pointTransactionId") =
    fieldName responseType STRING means "인연 충전 거래 ID (암호화)" example "XyZaBcD0"

fun balance(fieldName: String = "balance") =
    fieldName responseType NUMBER means "충전 후 인연 잔액" example "30"

fun chargedQuantity(fieldName: String = "chargedQuantity") =
    fieldName responseType NUMBER means "이번 충전으로 지급된 인연 수량" example "10"

fun paidAmount(fieldName: String = "paidAmount") =
    fieldName responseType NUMBER means "실제 결제된 금액 (원, 서버 재계산 기준)" example "1000"

fun approvalNumber(fieldName: String = "approvalNumber") =
    fieldName responseType STRING means "PG사 승인 번호" example "MOCK-550e8400-e29b-41d4-a716-446655440000"
