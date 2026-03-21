package com.konkuk.ma.vocabulary

import com.konkuk.ma.extension.NUMBER
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.responseType

// --- 회원 관련 필드 ---

fun memberId(fieldName: String = "memberId") =
    fieldName responseType NUMBER means "회원 ID" example "1"

fun category(fieldName: String = "category") =
    fieldName responseType STRING means "enum 코드" example "SEOUL"

fun displayName(fieldName: String = "displayName") =
    fieldName responseType STRING means "화면 표시명" example "서울"
