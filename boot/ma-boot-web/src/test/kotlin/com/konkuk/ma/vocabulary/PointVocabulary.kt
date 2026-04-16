package com.konkuk.ma.vocabulary

import com.konkuk.ma.extension.NUMBER
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.responseType

// --- 포인트 상품 관련 필드 ---

fun pointProductId(fieldName: String = "pointProductId") =
    fieldName responseType STRING means "포인트 상품 ID (인코딩)" example "abc123"

fun pointProductName(fieldName: String = "name") =
    fieldName responseType STRING means "상품명" example "인연 10개"

fun quantity(fieldName: String = "quantity") =
    fieldName responseType NUMBER means "인연 수량" example "10"

fun price(fieldName: String = "price") =
    fieldName responseType NUMBER means "가격 (원)" example "1000"
