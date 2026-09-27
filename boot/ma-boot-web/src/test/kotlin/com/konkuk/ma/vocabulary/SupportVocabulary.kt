package com.konkuk.ma.vocabulary

import com.konkuk.ma.domain.support.domain.NewInquiry
import com.konkuk.ma.extension.NUMBER
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.responseType

// --- 문의 작성 요청 필드 ---

fun inquiryTitle(fieldName: String = "title") =
    fieldName responseType STRING means "문의 제목 (최대 ${NewInquiry.MAX_TITLE_LENGTH}자)" example "로그인이 안됩니다"

fun inquiryContent(fieldName: String = "content") =
    fieldName responseType STRING means "문의 내용 (최대 ${NewInquiry.MAX_CONTENT_LENGTH}자)" example "비밀번호를 올바르게 입력해도 로그인이 되지 않습니다."

// --- 문의 작성 응답 필드 ---

fun inquiryId(fieldName: String = "inquiryId") =
    fieldName responseType NUMBER means "생성된 문의 ID" example "1"
