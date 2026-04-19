package com.konkuk.ma.vocabulary

import com.konkuk.ma.extension.BOOLEAN
import com.konkuk.ma.extension.NUMBER
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.responseType

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
