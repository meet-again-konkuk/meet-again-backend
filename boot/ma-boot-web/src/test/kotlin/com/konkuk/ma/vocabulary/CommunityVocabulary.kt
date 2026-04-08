package com.konkuk.ma.vocabulary

import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.extension.BOOLEAN
import com.konkuk.ma.extension.NUMBER
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.requestParam
import com.konkuk.ma.extension.responseType

// --- 게시글 작성 요청 필드 ---

fun newPostCategory(fieldName: String = "category") =
    fieldName responseType STRING means "게시글 카테고리 (SUCCESS_STORY, CHEER, COUNSELING)" example "CHEER"

fun newPostTitle(fieldName: String = "title") =
    fieldName responseType STRING means "게시글 제목 (최대 ${NewPost.MAX_TITLE_LENGTH}자)" example "안녕하세요"

fun newPostContent(fieldName: String = "content") =
    fieldName responseType STRING means "게시글 내용 (최대 ${NewPost.MAX_CONTENT_LENGTH}자)" example "반갑습니다"

// --- 게시글 작성 응답 필드 ---

fun newPostId(fieldName: String = "postId") =
    fieldName responseType NUMBER means "생성된 게시글 ID" example "1"

// --- 게시글 목록 응답 필드 ---

fun postId(fieldName: String = "data[].id") =
    fieldName responseType NUMBER means "게시글 ID" example "1"

fun postNickname(fieldName: String = "data[].nickname") =
    fieldName responseType STRING means "작성자 닉네임" example "테스트닉네임"

fun postCategory(fieldName: String = "data[].category") =
    fieldName responseType STRING means "게시글 카테고리" example "CHEER"

fun postTitle(fieldName: String = "data[].title") =
    fieldName responseType STRING means "게시글 제목" example "안녕하세요"

fun postContent(fieldName: String = "data[].content") =
    fieldName responseType STRING means "게시글 내용" example "반갑습니다"

fun postLikes(fieldName: String = "data[].likes") =
    fieldName responseType NUMBER means "좋아요 수" example "5"

fun postTimeAgo(fieldName: String = "data[].timeAgo") =
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

// --- 댓글 작성 요청 필드 ---

fun commentContent(fieldName: String = "content") =
    fieldName responseType STRING means "댓글 내용 (최대 ${NewComment.MAX_CONTENT_LENGTH}자)" example "좋은 글이네요!"

fun parentCommentId(fieldName: String = "parentCommentId") =
    fieldName responseType NUMBER means "부모 댓글 ID (대댓글인 경우, 일반 댓글이면 null)" example "1" isOptional true

// --- 댓글 작성 응답 필드 ---

fun commentId(fieldName: String = "commentId") =
    fieldName responseType NUMBER means "생성된 댓글 ID" example "1"

// --- 댓글 관련 Path Variable ---

fun postIdPath(fieldName: String = "postId") =
    fieldName requestParam "게시글 ID"

// --- 게시글 좋아요 응답 필드 ---

fun postLiked(fieldName: String = "liked") =
    fieldName responseType BOOLEAN means "좋아요 여부 (true: 좋아요, false: 좋아요 취소)" example "true"

fun postLikeCount(fieldName: String = "likeCount") =
    fieldName responseType NUMBER means "게시글 좋아요 수" example "3"

// --- 댓글 좋아요 응답 필드 ---

fun commentLiked(fieldName: String = "liked") =
    fieldName responseType BOOLEAN means "좋아요 여부 (true: 좋아요, false: 좋아요 취소)" example "true"

fun commentLikeCount(fieldName: String = "likeCount") =
    fieldName responseType NUMBER means "댓글 좋아요 수" example "3"

// --- 댓글 좋아요 관련 Path Variable ---

fun commentIdPath(fieldName: String = "commentId") =
    fieldName requestParam "댓글 ID"
