package com.konkuk.ma.vocabulary

import com.konkuk.ma.extension.BOOLEAN
import com.konkuk.ma.extension.NUMBER
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.requestParam
import com.konkuk.ma.extension.responseType

// --- 게시글 목록 응답 필드 ---

fun postId(fieldName: String = "posts[].id") =
    fieldName responseType NUMBER means "게시글 ID" example "1"

fun postNickname(fieldName: String = "posts[].nickname") =
    fieldName responseType STRING means "작성자 닉네임" example "테스트닉네임"

fun postCategory(fieldName: String = "posts[].category") =
    fieldName responseType STRING means "게시글 카테고리" example "CHEER"

fun postTitle(fieldName: String = "posts[].title") =
    fieldName responseType STRING means "게시글 제목" example "안녕하세요"

fun postContent(fieldName: String = "posts[].content") =
    fieldName responseType STRING means "게시글 내용" example "반갑습니다"

fun postLikes(fieldName: String = "posts[].likes") =
    fieldName responseType NUMBER means "좋아요 수" example "5"

fun postComments(fieldName: String = "posts[].comments") =
    fieldName responseType NUMBER means "댓글 수" example "3"

fun postTimeAgo(fieldName: String = "posts[].timeAgo") =
    fieldName responseType STRING means "작성 경과 시간" example "5분 전"

fun postsHasNext(fieldName: String = "hasNext") =
    fieldName responseType BOOLEAN means "다음 페이지 존재 여부" example "true"

fun postsNextCursorId(fieldName: String = "nextCursorId") =
    fieldName responseType NUMBER means "다음 페이지 커서 ID (마지막 페이지면 null)" example "1"

// --- 게시글 목록 요청 파라미터 ---

fun categoryParam(fieldName: String = "category") =
    fieldName requestParam "게시글 카테고리 (SUCCESS_STORY, CHEER, COUNSELING)"

fun cursorIdParam(fieldName: String = "cursorId") =
    fieldName requestParam "마지막으로 본 게시글 ID (첫 페이지는 미전송)" isOptional true

fun pageParam(fieldName: String = "page") =
    fieldName requestParam "페이지 번호 (기본값: 0)" isOptional true

fun sizeParam(fieldName: String = "size") =
    fieldName requestParam "페이지 크기 (기본값: 20)" isOptional true
