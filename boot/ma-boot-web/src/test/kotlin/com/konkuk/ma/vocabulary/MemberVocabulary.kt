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

// --- 회원 프로필 공통 필드 (다양한 응답 리소스에서 path만 바꿔 재사용) ---

fun memberName(fieldName: String) =
    fieldName responseType STRING means "이름" example "김만남"

fun memberNickname(fieldName: String) =
    fieldName responseType STRING means "닉네임" example "테스트닉네임"

fun memberProfileImageUrl(fieldName: String) =
    fieldName responseType STRING means "프로필 이미지 URL" example "https://example.com/image.jpg"

fun memberIsWithdrawn(fieldName: String) =
    fieldName responseType BOOLEAN means "탈퇴 여부" example "false"
