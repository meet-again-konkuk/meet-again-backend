package com.konkuk.ma.vocabulary

import com.konkuk.ma.extension.BOOLEAN
import com.konkuk.ma.extension.DATETIME
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.responseType

// --- 인증 관련 필드 ---

fun accessToken(fieldName: String = "accessToken") =
    fieldName responseType STRING means "액세스 토큰" example "eyJhbGciOi..."

fun refreshToken(fieldName: String = "refreshToken") =
    fieldName responseType STRING means "리프레시 토큰" example "eyJhbGciOi..."

fun verificationCode(fieldName: String = "verificationCode") =
    fieldName responseType STRING means "인증 코드" example "123456"

fun verified(fieldName: String = "verified") =
    fieldName responseType BOOLEAN means "인증 성공 여부" example "true"

fun gender(fieldName: String = "gender") =
    fieldName responseType STRING means "성별" example "MALE"

fun birthDate(fieldName: String = "birthDate") =
    fieldName responseType STRING means "생년월일" example "1990-01-01"

fun region(fieldName: String = "region") =
    fieldName responseType STRING means "거주 지역" example "SEOUL"

fun highSchool(fieldName: String = "highSchool") =
    fieldName responseType STRING means "고등학교" example "테스트고등학교"

fun university(fieldName: String = "university") =
    fieldName responseType STRING means "대학교" example "테스트대학교"

fun loginStatus(fieldName: String = "status") =
    fieldName responseType STRING means "로그인 상태 (ACTIVE / WITHDRAWAL_PENDING)" example "ACTIVE"

fun withdrawalRequestedAt(fieldName: String = "withdrawalRequestedAt") =
    fieldName responseType DATETIME means "탈퇴 신청 시각 (pending 상태일 때만)" example "2026-05-01T10:30:00"

fun withdrawalExpiresAt(fieldName: String = "withdrawalExpiresAt") =
    fieldName responseType DATETIME means "탈퇴 유예 만료 시각 (pending 상태일 때만)" example "2026-05-08T10:30:00"
