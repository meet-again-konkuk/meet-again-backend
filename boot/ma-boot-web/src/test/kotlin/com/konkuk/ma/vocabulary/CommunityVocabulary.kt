package com.konkuk.ma.vocabulary

import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.domain.report.NewReport
import com.konkuk.ma.extension.ARRAY
import com.konkuk.ma.extension.BOOLEAN
import com.konkuk.ma.extension.DATETIME
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

// --- 게시글 상세 응답 필드 ---

fun detailPostId(fieldName: String = "id") =
    fieldName responseType NUMBER means "게시글 ID" example "1"

fun detailNickname(fieldName: String = "nickname") =
    fieldName responseType STRING means "작성자 닉네임" example "테스트닉네임"

fun detailCategory(fieldName: String = "category") =
    fieldName responseType STRING means "게시글 카테고리" example "CHEER"

fun detailTitle(fieldName: String = "title") =
    fieldName responseType STRING means "게시글 제목" example "안녕하세요"

fun detailContent(fieldName: String = "content") =
    fieldName responseType STRING means "게시글 내용" example "반갑습니다"

fun detailLikes(fieldName: String = "likes") =
    fieldName responseType NUMBER means "좋아요 수" example "5"

fun detailTimeAgo(fieldName: String = "timeAgo") =
    fieldName responseType STRING means "작성 경과 시간" example "5분 전"

fun detailComments(fieldName: String = "comments[]") =
    fieldName responseType ARRAY means "댓글 목록"

fun detailCommentId(fieldName: String = "comments[].id") =
    fieldName responseType NUMBER means "댓글 ID" example "1"

fun detailCommentNickname(fieldName: String = "comments[].nickname") =
    fieldName responseType STRING means "댓글 작성자 닉네임" example "댓글작성자"

fun detailCommentContent(fieldName: String = "comments[].content") =
    fieldName responseType STRING means "댓글 내용" example "좋은 글이네요!"

fun detailCommentLikes(fieldName: String = "comments[].likes") =
    fieldName responseType NUMBER means "댓글 좋아요 수" example "2"

fun detailCommentTimeAgo(fieldName: String = "comments[].timeAgo") =
    fieldName responseType STRING means "댓글 작성 경과 시간" example "3분 전"

fun detailReplies(fieldName: String = "comments[].replies[]") =
    fieldName responseType ARRAY means "대댓글 목록"

fun detailReplyId(fieldName: String = "comments[].replies[].id") =
    fieldName responseType NUMBER means "대댓글 ID" example "2"

fun detailReplyNickname(fieldName: String = "comments[].replies[].nickname") =
    fieldName responseType STRING means "대댓글 작성자 닉네임" example "대댓글작성자"

fun detailReplyContent(fieldName: String = "comments[].replies[].content") =
    fieldName responseType STRING means "대댓글 내용" example "감사합니다!"

fun detailReplyLikes(fieldName: String = "comments[].replies[].likes") =
    fieldName responseType NUMBER means "대댓글 좋아요 수" example "1"

fun detailReplyTimeAgo(fieldName: String = "comments[].replies[].timeAgo") =
    fieldName responseType STRING means "대댓글 작성 경과 시간" example "1분 전"

fun detailRemainingReplyCount(fieldName: String = "comments[].remainingReplyCount") =
    fieldName responseType NUMBER means "남은 대댓글 수" example "3"

// --- 게시글 상세 관련 Path Variable ---

fun postDetailIdPath(fieldName: String = "id") =
    fieldName requestParam "게시글 ID"

// --- 댓글 상세 응답 필드 ---

fun commentDetailId(fieldName: String = "id") =
    fieldName responseType NUMBER means "댓글 ID" example "1"

fun commentDetailNickname(fieldName: String = "nickname") =
    fieldName responseType STRING means "댓글 작성자 닉네임" example "댓글작성자"

fun commentDetailContent(fieldName: String = "content") =
    fieldName responseType STRING means "댓글 내용" example "좋은 글이네요!"

fun commentDetailLikes(fieldName: String = "likes") =
    fieldName responseType NUMBER means "댓글 좋아요 수" example "2"

fun commentDetailTimeAgo(fieldName: String = "timeAgo") =
    fieldName responseType STRING means "댓글 작성 경과 시간" example "3분 전"

fun commentDetailReplies(fieldName: String = "replies[]") =
    fieldName responseType ARRAY means "대댓글 목록"

fun commentDetailReplyId(fieldName: String = "replies[].id") =
    fieldName responseType NUMBER means "대댓글 ID" example "2"

fun commentDetailReplyNickname(fieldName: String = "replies[].nickname") =
    fieldName responseType STRING means "대댓글 작성자 닉네임" example "대댓글작성자"

fun commentDetailReplyContent(fieldName: String = "replies[].content") =
    fieldName responseType STRING means "대댓글 내용" example "감사합니다!"

fun commentDetailReplyLikes(fieldName: String = "replies[].likes") =
    fieldName responseType NUMBER means "대댓글 좋아요 수" example "1"

fun commentDetailReplyTimeAgo(fieldName: String = "replies[].timeAgo") =
    fieldName responseType STRING means "대댓글 작성 경과 시간" example "1분 전"

fun commentDetailRemainingReplyCount(fieldName: String = "remainingReplyCount") =
    fieldName responseType NUMBER means "남은 대댓글 수" example "0"

// --- REQ-011: 조회 응답 사용자 상태 필드 ---

// 게시글 목록 data[]
fun postLikedByMe(fieldName: String = "data[].likedByMe") =
    fieldName responseType BOOLEAN means "조회자의 활성 좋아요 여부" example "true"

fun postIsMine(fieldName: String = "data[].isMine") =
    fieldName responseType BOOLEAN means "조회자가 작성한 게시글인지 여부" example "false"

fun postCommentCount(fieldName: String = "data[].commentCount") =
    fieldName responseType NUMBER means "댓글 수 (삭제 제외 루트+대댓글 합)" example "3"

// 게시글 상세(post)
fun detailLikedByMe(fieldName: String = "likedByMe") =
    fieldName responseType BOOLEAN means "조회자의 활성 좋아요 여부" example "true"

fun detailIsMine(fieldName: String = "isMine") =
    fieldName responseType BOOLEAN means "조회자가 작성한 게시글인지 여부" example "false"

// 게시글 상세 comments[]
fun detailCommentLikedByMe(fieldName: String = "comments[].likedByMe") =
    fieldName responseType BOOLEAN means "조회자의 활성 좋아요 여부" example "true"

fun detailCommentIsMine(fieldName: String = "comments[].isMine") =
    fieldName responseType BOOLEAN means "조회자가 작성한 댓글인지 여부" example "false"

// 게시글 상세 comments[].replies[]
fun detailReplyLikedByMe(fieldName: String = "comments[].replies[].likedByMe") =
    fieldName responseType BOOLEAN means "조회자의 활성 좋아요 여부" example "true"

fun detailReplyIsMine(fieldName: String = "comments[].replies[].isMine") =
    fieldName responseType BOOLEAN means "조회자가 작성한 대댓글인지 여부" example "false"

// 루트 댓글 상세(comment)
fun commentDetailLikedByMe(fieldName: String = "likedByMe") =
    fieldName responseType BOOLEAN means "조회자의 활성 좋아요 여부" example "true"

fun commentDetailIsMine(fieldName: String = "isMine") =
    fieldName responseType BOOLEAN means "조회자가 작성한 댓글인지 여부" example "false"

// 루트 댓글 상세 replies[]
fun commentDetailReplyLikedByMe(fieldName: String = "replies[].likedByMe") =
    fieldName responseType BOOLEAN means "조회자의 활성 좋아요 여부" example "true"

fun commentDetailReplyIsMine(fieldName: String = "replies[].isMine") =
    fieldName responseType BOOLEAN means "조회자가 작성한 대댓글인지 여부" example "false"

// --- REQ-014: 차단한 작성자 표식 (댓글/대댓글 공통) ---

// fieldName 파라미터로 comments[]/replies[]/루트 등 위치를 바꿔 재사용한다.
fun blockedAuthor(fieldName: String = "blockedAuthor") =
    fieldName responseType BOOLEAN means
        "조회자가 차단한 작성자의 콘텐츠 여부 (true면 content는 \"차단한 사용자의 댓글입니다.\" placeholder로 대체)" example "false"

// --- REQ-014: 신고 요청 필드 ---

fun reportReason(fieldName: String = "reason") =
    fieldName responseType STRING means
        "신고 사유 (SPAM, HARASSMENT, HATE, SEXUAL_CONTENT, PRIVACY, OTHER)" example "SPAM"

fun reportDetail(fieldName: String = "detail") =
    fieldName responseType STRING means
        "신고 상세 사유 (최대 ${NewReport.MAX_DETAIL_LENGTH}자, 선택)" example "욕설이 포함되어 있습니다." isOptional true

// --- REQ-014: 신고 응답 필드 ---

fun reportId(fieldName: String = "reportId") =
    fieldName responseType NUMBER means "생성된 신고 ID" example "1"

fun reportStatus(fieldName: String = "status") =
    fieldName responseType STRING means
        "신고 처리 상태 (RECEIVED, REVIEWING, ACTIONED, DISMISSED)" example "RECEIVED"

// --- REQ-014: 차단 응답 필드 (단건) ---

fun blockId(fieldName: String = "blockId") =
    fieldName responseType NUMBER means "차단 ID" example "1"

fun blocked(fieldName: String = "blocked") =
    fieldName responseType BOOLEAN means "차단 여부 (항상 true)" example "true"

fun blockedNickname(fieldName: String = "blockedNickname") =
    fieldName responseType STRING means "차단한 작성자 닉네임" example "차단된작성자"

// --- REQ-014: 차단 목록 응답 필드 ---

fun blocksArray(fieldName: String = "blocks[]") =
    fieldName responseType ARRAY means "차단 목록"

fun blocksBlockId(fieldName: String = "blocks[].blockId") =
    fieldName responseType NUMBER means "차단 ID" example "1"

fun blocksNickname(fieldName: String = "blocks[].nickname") =
    fieldName responseType STRING means "차단한 작성자 닉네임" example "차단된작성자"

fun blocksBlockedAt(fieldName: String = "blocks[].blockedAt") =
    fieldName responseType DATETIME means "차단한 시각" example "2026-07-07T10:30:00"

// --- REQ-014: 차단 관련 Path Variable ---

fun blockIdPath(fieldName: String = "blockId") =
    fieldName requestParam "차단 ID"
