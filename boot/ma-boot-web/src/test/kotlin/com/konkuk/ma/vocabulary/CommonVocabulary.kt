package com.konkuk.ma.vocabulary

import com.konkuk.ma.extension.BOOLEAN
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.responseType

// --- 공통 필드 ---

fun email(fieldName: String = "email") =
    fieldName responseType STRING means "이메일" example "user@example.com"

fun password(fieldName: String = "password") =
    fieldName responseType STRING means "비밀번호" example "password123"

fun nickname(fieldName: String = "nickname") =
    fieldName responseType STRING means "닉네임" example "testuser"

fun name(fieldName: String = "name") =
    fieldName responseType STRING means "이름" example "김테스트"

fun phoneNumber(fieldName: String = "phoneNumber") =
    fieldName responseType STRING means "휴대폰 번호" example "01012345678"

fun message(fieldName: String = "message", description: String = "응답 메시지") =
    fieldName responseType STRING means description example "요청이 완료되었습니다."

fun duplicated(fieldName: String = "duplicated") =
    fieldName responseType BOOLEAN means "중복 여부" example "false"
