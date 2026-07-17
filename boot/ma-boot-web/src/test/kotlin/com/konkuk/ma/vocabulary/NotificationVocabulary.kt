package com.konkuk.ma.vocabulary

import com.konkuk.ma.domain.notification.domain.NotificationType
import com.konkuk.ma.extension.BOOLEAN
import com.konkuk.ma.extension.DATETIME
import com.konkuk.ma.extension.ENUM
import com.konkuk.ma.extension.NUMBER
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.requestParam
import com.konkuk.ma.extension.responseType

// --- 알림 목록 응답 필드 (data[]) ---

fun notificationId(fieldName: String = "data[].id") =
    fieldName responseType NUMBER means "알림 ID" example "12"

fun notificationType(fieldName: String = "data[].type") =
    fieldName responseType ENUM(NotificationType::class) means
        "알림 유형 (COMMENT_ON_POST: 내 게시글에 댓글, REPLY_ON_COMMENT: 내 댓글에 답글)"

fun notificationActorId(fieldName: String = "data[].actorId") =
    fieldName responseType NUMBER means "알림을 발생시킨 회원 ID" example "5"

fun notificationActorNickname(fieldName: String = "data[].actorNickname") =
    fieldName responseType STRING means "알림을 발생시킨 회원 닉네임 (탈퇴 회원은 \"알 수 없음\")" example "홍길동"

fun notificationPostId(fieldName: String = "data[].postId") =
    fieldName responseType NUMBER means "관련 게시글 ID" example "30"

fun notificationCommentId(fieldName: String = "data[].commentId") =
    fieldName responseType NUMBER means "관련 댓글 ID" example "77"

fun notificationRead(fieldName: String = "data[].read") =
    fieldName responseType BOOLEAN means "읽음 여부" example "false"

fun notificationCreatedDate(fieldName: String = "data[].createdDate") =
    fieldName responseType DATETIME means "알림 생성 시각" example "2026-07-17T10:20:30"

// --- 알림 목록 커서 페이징 필드 ---

fun notificationsHasNext(fieldName: String = "hasNext") =
    fieldName responseType BOOLEAN means "다음 페이지 존재 여부" example "true"

fun notificationsNextCursorId(fieldName: String = "nextCursorId") =
    fieldName responseType NUMBER means "다음 페이지 커서 ID (마지막 페이지면 null)" example "12"

// --- 안읽음 개수 응답 필드 ---

fun unreadCount(fieldName: String = "count") =
    fieldName responseType NUMBER means "안읽음 알림 개수" example "3"

// --- 알림 목록 요청 파라미터 (size 는 CommunityVocabulary.sizeParam 재사용) ---

fun notificationCursorIdParam(fieldName: String = "cursorId") =
    fieldName requestParam "마지막으로 본 알림 ID (첫 페이지는 미전송)" isOptional true

// --- 알림 관련 Path Variable ---

fun notificationIdPath(fieldName: String = "notificationId") =
    fieldName requestParam "알림 ID"
