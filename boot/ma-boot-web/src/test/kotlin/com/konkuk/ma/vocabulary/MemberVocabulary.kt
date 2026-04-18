package com.konkuk.ma.vocabulary

import com.konkuk.ma.extension.BOOLEAN
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.responseType

// --- 회원 관련 필드 ---

fun memberId(fieldName: String = "memberId") =
    fieldName responseType STRING means "회원 ID (인코딩)" example "abc123"

fun category(fieldName: String = "category") =
    fieldName responseType STRING means "enum 코드" example "SEOUL"

fun displayName(fieldName: String = "displayName") =
    fieldName responseType STRING means "화면 표시명" example "서울"

fun profileImageUrl(fieldName: String) =
    fieldName responseType STRING means "프로필 이미지 URL" example "https://example.com/image.jpg"

fun isWithdrawn(fieldName: String) =
    fieldName responseType BOOLEAN means "탈퇴 여부" example "false"
