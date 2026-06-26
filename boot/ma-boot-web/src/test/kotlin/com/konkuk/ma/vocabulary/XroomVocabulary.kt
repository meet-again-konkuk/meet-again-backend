package com.konkuk.ma.vocabulary

import com.konkuk.ma.extension.DATETIME
import com.konkuk.ma.extension.NUMBER
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.requestParam
import com.konkuk.ma.extension.responseType

// --- X룸 관련 필드 ---

fun xroomId(fieldName: String = "xroomId") =
    fieldName responseType STRING means "X룸 ID (인코딩)" example "abc123"

fun targetInfoIdParam(fieldName: String = "targetInfoId") =
    fieldName requestParam "찾는 사람 정보 ID (인코딩)"

fun finalMessage(fieldName: String = "finalMessage") =
    fieldName responseType STRING means "마지막으로 전하는 메시지" example "고마웠어"

fun xroomTitle(fieldName: String = "title") =
    fieldName responseType STRING means "방 제목" example "기억의 방"

fun recipientName(fieldName: String = "recipientName") =
    fieldName responseType STRING means "수신자(받는 사람) 이름" example "김만남"

fun memoryCount(fieldName: String = "memoryCount") =
    fieldName responseType NUMBER means "방에 담긴 기억 개수" example "0"

fun xroomUpdatedAt(fieldName: String = "updatedAt") =
    fieldName responseType DATETIME means "방 마지막 수정 시각" example "2026-06-26T10:30:00"
