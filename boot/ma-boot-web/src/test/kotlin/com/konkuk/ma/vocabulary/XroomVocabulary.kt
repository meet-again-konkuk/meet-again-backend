package com.konkuk.ma.vocabulary

import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.requestParam
import com.konkuk.ma.extension.responseType

// --- X룸 관련 필드 ---

fun xroomId(fieldName: String = "xroomId") =
    fieldName responseType STRING means "X룸 ID (인코딩)" example "abc123"

fun targetInfoIdParam(fieldName: String = "targetInfoId") =
    fieldName requestParam "찾는 사람 정보 ID (인코딩)"
