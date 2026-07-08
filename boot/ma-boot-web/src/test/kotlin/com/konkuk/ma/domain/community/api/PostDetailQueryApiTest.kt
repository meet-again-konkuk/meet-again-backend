package com.konkuk.ma.domain.community.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.community.application.PostQueryService
import com.konkuk.ma.domain.community.domain.CommentWithAuthor
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.PostDetail
import com.konkuk.ma.domain.community.domain.ReplyWithAuthor
import com.konkuk.ma.domain.community.fixture.CommentFixture
import com.konkuk.ma.domain.community.fixture.PostFixture
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.blockedAuthor
import com.konkuk.ma.vocabulary.detailCategory
import com.konkuk.ma.vocabulary.detailCommentContent
import com.konkuk.ma.vocabulary.detailCommentId
import com.konkuk.ma.vocabulary.detailCommentIsMine
import com.konkuk.ma.vocabulary.detailCommentLikedByMe
import com.konkuk.ma.vocabulary.detailCommentLikes
import com.konkuk.ma.vocabulary.detailCommentNickname
import com.konkuk.ma.vocabulary.detailCommentTimeAgo
import com.konkuk.ma.vocabulary.detailComments
import com.konkuk.ma.vocabulary.detailContent
import com.konkuk.ma.vocabulary.detailIsMine
import com.konkuk.ma.vocabulary.detailLikedByMe
import com.konkuk.ma.vocabulary.detailLikes
import com.konkuk.ma.vocabulary.detailNickname
import com.konkuk.ma.vocabulary.detailPostId
import com.konkuk.ma.vocabulary.detailRemainingReplyCount
import com.konkuk.ma.vocabulary.detailReplies
import com.konkuk.ma.vocabulary.detailReplyContent
import com.konkuk.ma.vocabulary.detailReplyId
import com.konkuk.ma.vocabulary.detailReplyIsMine
import com.konkuk.ma.vocabulary.detailReplyLikedByMe
import com.konkuk.ma.vocabulary.detailReplyLikes
import com.konkuk.ma.vocabulary.detailReplyNickname
import com.konkuk.ma.vocabulary.detailReplyTimeAgo
import com.konkuk.ma.vocabulary.detailTimeAgo
import com.konkuk.ma.vocabulary.detailTitle
import com.konkuk.ma.vocabulary.postDetailIdPath
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(PostQueryApi::class)
@BaseApiTest
class PostDetailQueryApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val postQueryService: PostQueryService,
) : FunSpec({

    test("게시글 상세 조회 API 문서화") {
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
            remainingReplyCount = 3,
            likedByMe = true,
            isMine = false,
        )
        val postDetail = PostDetail(
            post = PostFixture.create(category = PostCategory.CHEER, title = "안녕하세요", content = "반갑습니다"),
            nickname = "테스트닉네임",
            likeCount = 5,
            comments = listOf(commentWithAuthor),
            likedByMe = true,
            isMine = true,
        )

        every { postQueryService.findDetail(1L, any<Long>()) } returns postDetail

        // When & Then
        mockMvc.getJson("/api/community/posts/{id}", 1L)
            .andExpect { status { isOk() } }
            .andDocument(
                "community/find-post-detail",
                pathVariables(
                    postDetailIdPath(),
                ),
                responseBody(
                    detailPostId(),
                    detailNickname(),
                    detailCategory(),
                    detailTitle(),
                    detailContent(),
                    detailLikes(),
                    detailTimeAgo(),
                    detailLikedByMe(),
                    detailIsMine(),
                    detailComments(),
                    detailCommentId(),
                    detailCommentNickname(),
                    detailCommentContent(),
                    detailCommentLikes(),
                    detailCommentTimeAgo(),
                    detailCommentLikedByMe(),
                    detailCommentIsMine(),
                    blockedAuthor("comments[].blockedAuthor"),
                    detailReplies(),
                    detailReplyId(),
                    detailReplyNickname(),
                    detailReplyContent(),
                    detailReplyLikes(),
                    detailReplyTimeAgo(),
                    detailReplyLikedByMe(),
                    detailReplyIsMine(),
                    blockedAuthor("comments[].replies[].blockedAuthor"),
                    detailRemainingReplyCount(),
                ),
            )
    }

    test("존재하지 않는 게시글 조회 시 404를 반환한다") {
        // Given
        every { postQueryService.findDetail(999L, any<Long>()) } throws
            EntityNotFoundException(EntityType.COMMUNITY_POST, "999")

        // When & Then
        mockMvc.getJson("/api/community/posts/{id}", 999L)
            .andExpect { status { isNotFound() } }
    }
})
