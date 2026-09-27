package com.konkuk.ma.domain.community.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.community.application.CommentQueryService
import com.konkuk.ma.domain.community.domain.CommentWithAuthor
import com.konkuk.ma.domain.community.domain.ReplyWithAuthor
import com.konkuk.ma.domain.community.fixture.CommentFixture
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.blockedAuthor
import com.konkuk.ma.vocabulary.commentDetailContent
import com.konkuk.ma.vocabulary.commentDetailId
import com.konkuk.ma.vocabulary.commentDetailIsMine
import com.konkuk.ma.vocabulary.commentDetailLikedByMe
import com.konkuk.ma.vocabulary.commentDetailLikes
import com.konkuk.ma.vocabulary.commentDetailNickname
import com.konkuk.ma.vocabulary.commentDetailRemainingReplyCount
import com.konkuk.ma.vocabulary.commentDetailReplies
import com.konkuk.ma.vocabulary.commentDetailReplyContent
import com.konkuk.ma.vocabulary.commentDetailReplyId
import com.konkuk.ma.vocabulary.commentDetailReplyIsMine
import com.konkuk.ma.vocabulary.commentDetailReplyLikedByMe
import com.konkuk.ma.vocabulary.commentDetailReplyLikes
import com.konkuk.ma.vocabulary.commentDetailReplyNickname
import com.konkuk.ma.vocabulary.commentDetailReplyTimeAgo
import com.konkuk.ma.vocabulary.commentDetailTimeAgo
import com.konkuk.ma.vocabulary.commentIdPath
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(CommentQueryApi::class)
@BaseApiTest
class CommentQueryApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val commentQueryService: CommentQueryService,
) : FunSpec({

    test("댓글 상세 조회 API 문서화") {
        // Given
        val reply = ReplyWithAuthor(
            comment = CommentFixture.create(id = 2L, parentCommentId = 1L, content = "감사합니다!"),
            nickname = "대댓글작성자",
            likeCount = 1,
            likedByMe = false,
            isMine = false,
        )
        val commentWithAuthor = CommentWithAuthor(
            comment = CommentFixture.create(id = 1L, content = "좋은 글이네요!"),
            nickname = "댓글작성자",
            likeCount = 2,
            replies = listOf(reply),
            remainingReplyCount = 0,
            likedByMe = true,
            isMine = false,
        )

        every { commentQueryService.findDetail(1L, any<Long>()) } returns commentWithAuthor

        // When & Then
        mockMvc.getJson("/api/community/comments/{commentId}", 1L)
            .andExpect { status { isOk() } }
            .andDocument(
                "community/find-comment-detail",
                pathVariables(
                    commentIdPath(),
                ),
                responseBody(
                    commentDetailId(),
                    commentDetailNickname(),
                    commentDetailContent(),
                    commentDetailLikes(),
                    commentDetailTimeAgo(),
                    commentDetailLikedByMe(),
                    commentDetailIsMine(),
                    blockedAuthor(),
                    commentDetailReplies(),
                    commentDetailReplyId(),
                    commentDetailReplyNickname(),
                    commentDetailReplyContent(),
                    commentDetailReplyLikes(),
                    commentDetailReplyTimeAgo(),
                    commentDetailReplyLikedByMe(),
                    commentDetailReplyIsMine(),
                    blockedAuthor("replies[].blockedAuthor"),
                    commentDetailRemainingReplyCount(),
                ),
            )
    }

    test("존재하지 않는 댓글 조회 시 404를 반환한다") {
        // Given
        every { commentQueryService.findDetail(999L, any<Long>()) } throws
            EntityNotFoundException(EntityType.COMMUNITY_COMMENT, "999")

        // When & Then
        mockMvc.getJson("/api/community/comments/{commentId}", 999L)
            .andExpect { status { isNotFound() } }
    }
})
