package com.konkuk.ma.vocabulary

import com.konkuk.ma.extension.ARRAY
import com.konkuk.ma.extension.BOOLEAN
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

fun senderName(fieldName: String = "senderName") =
    fieldName responseType STRING means "보낸 사람(작성자) 이름" example "김보냄"

fun template(fieldName: String = "template") =
    fieldName responseType STRING means "방 템플릿" example "chat_memory"

fun xroomMemories(fieldName: String = "memories") =
    fieldName responseType ARRAY means "방에 담긴 기억 목록"

// --- 기억(Memory) 관련 필드 ---

fun memoryId(fieldName: String = "memoryId") =
    fieldName responseType STRING means "기억 ID (인코딩)" example "mem123"

fun memoryTitle(fieldName: String = "title") =
    fieldName responseType STRING means "기억 제목" example "첫 만남"

fun eventDate(fieldName: String = "eventDate") =
    fieldName responseType STRING means "기억의 시점, precision별 부분 날짜" example "2019-05-10"

fun eventDatePrecision(fieldName: String = "eventDatePrecision") =
    fieldName responseType STRING means "시점 정밀도 (YEAR|MONTH|DAY)" example "DAY"

fun location(fieldName: String = "location") =
    fieldName responseType STRING means "장소" example "서울"

fun emotionTags(fieldName: String = "emotionTags") =
    fieldName responseType ARRAY means "감정 태그 목록" example "[\"설렘\", \"행복\"]"

fun memoryText(fieldName: String = "text") =
    fieldName responseType STRING means "짧은 글 (letter와 상호배타)" example "그날의 기억"

fun memoryLetter(fieldName: String = "letter") =
    fieldName responseType STRING means "편지 (text와 상호배타)" example "오랜만에 편지를 써본다"

fun photoUrl(fieldName: String = "photoUrl") =
    fieldName responseType STRING means "사진 서빙 URL (/files/...), 없으면 null" example "/files/xroom/1/memory/1/photo.jpg"

fun deleted(fieldName: String = "deleted") =
    fieldName responseType BOOLEAN means "삭제 여부 (true 고정)" example "true"

// --- 기억 사진(Media) 관련 필드 ---

fun mediaId(fieldName: String = "mediaId") =
    fieldName responseType STRING means "미디어(사진) ID (인코딩)" example "media123"

fun thumbnailUrl(fieldName: String = "thumbnailUrl") =
    fieldName responseType STRING means "썸네일 서빙 URL (/files/...), 없으면 null" example "/files/xroom/1/memory/1/thumb.jpg"

fun photoDeleted(fieldName: String = "photoDeleted") =
    fieldName responseType BOOLEAN means "사진 삭제 여부 (true 고정)" example "true"

// --- X룸 관련 Path Variable ---

fun xroomIdPath(fieldName: String = "xroomId") =
    fieldName requestParam "X룸 ID (인코딩)"

fun memoryIdPath(fieldName: String = "memoryId") =
    fieldName requestParam "기억 ID (인코딩)"
