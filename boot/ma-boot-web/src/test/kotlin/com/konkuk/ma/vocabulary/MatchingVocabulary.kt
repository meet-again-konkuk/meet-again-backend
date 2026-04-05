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

// --- 매칭 결과 관련 필드 ---

fun matchingResultId(fieldName: String = "matchingResults[].matchingResultId") =
    fieldName responseType NUMBER means "매칭 결과 ID" example "1"

fun matchingTargetName(fieldName: String = "matchingResults[].targetName") =
    fieldName responseType STRING means "매칭된 상대의 이름" example "김만남"

fun matchingTargetNickname(fieldName: String = "matchingResults[].targetNickname") =
    fieldName responseType STRING means "매칭된 상대의 닉네임" example "테스트닉네임"

fun profileImageUrl(fieldName: String = "matchingResults[].profileImageUrl") =
    fieldName responseType STRING means "매칭된 상대의 프로필 이미지 URL" example "https://example.com/image.jpg"

fun remainingDays(fieldName: String = "matchingResults[].remainingDays") =
    fieldName responseType NUMBER means "매칭 결과 노출 잔여일" example "25"

fun matchRate(fieldName: String = "matchingResults[].matchRate") =
    fieldName responseType NUMBER means "매칭률 (%)" example "75"
