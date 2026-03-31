package com.konkuk.ma.vocabulary

import com.konkuk.ma.extension.NUMBER
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.responseType

// --- 매칭 관련 필드 ---

fun targetInfoId(fieldName: String = "targetInfoId") =
    fieldName responseType STRING means "찾는 사람 정보 ID (인코딩)" example "abc123"

fun registerEmail(fieldName: String = "registerEmail") =
    fieldName responseType STRING means "등록자 email" example "test@example.com"

fun targetName(fieldName: String = "name") =
    fieldName responseType STRING means "찾는 사람의 이름" example "김만남"

fun middleNumber(fieldName: String = "middleNumber") =
    fieldName responseType STRING means "전화번호 중간자리" example "1234"

fun lastNumber(fieldName: String = "lastNumber") =
    fieldName responseType STRING means "전화번호 뒷자리" example "5678"

fun year(fieldName: String = "year") =
    fieldName responseType NUMBER means "생년" example "1995"

fun month(fieldName: String = "month") =
    fieldName responseType NUMBER means "생월" example "5"

fun day(fieldName: String = "day") =
    fieldName responseType NUMBER means "생일" example "15"

fun targetRegion(fieldName: String = "region") =
    fieldName responseType STRING means "지역" example "SEOUL"
