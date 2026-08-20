package com.konkuk.ma.vocabulary

import com.konkuk.ma.extension.BOOLEAN
import com.konkuk.ma.extension.DATETIME
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
    fieldName responseType STRING means "프로필 이미지 URL" example "/files/member/thumbnail/1/thumb_photo.jpg"

fun isWithdrawn(fieldName: String) =
    fieldName responseType BOOLEAN means "탈퇴 여부" example "false"

fun cancelledAt(fieldName: String = "cancelledAt") =
    fieldName responseType DATETIME means "탈퇴 신청 복구 시각" example "2026-05-08T10:30:00"

// --- 내 프로필(GET/PATCH /api/members/me) 필드 ---
// memberId · profileImageUrl 은 위 정의를 재사용한다.
// email · nickname · name · phoneNumber 는 CommonVocabulary,
// gender · birthDate · region · highSchool · university 는 AuthVocabulary 의 정의를 재사용한다
// (같은 com.konkuk.ma.vocabulary 패키지라 여기에 다시 정의하면 Conflicting overloads 가 된다).
