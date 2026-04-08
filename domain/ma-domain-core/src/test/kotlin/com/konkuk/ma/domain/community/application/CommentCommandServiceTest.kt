package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.community.exception.ReplyDepthExceededException
import com.konkuk.ma.domain.community.fixture.CommentFixture
import com.konkuk.ma.domain.community.fixture.NewCommentFixture
import com.konkuk.ma.domain.community.fixture.PostFixture
import com.konkuk.ma.exception.EntityNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class CommentCommandServiceTest : FunSpec({

    val postQueryRepository = mockk<PostQueryRepository>()
    val postCommandRepository = mockk<PostCommandRepository>()
    val commentCommandRepository = mockk<CommentCommandRepository>()
    val commentQueryRepository = mockk<CommentQueryRepository>()
    val service = CommentCommandService(
        postQueryRepository,
        postCommandRepository,
        commentCommandRepository,
        commentQueryRepository,
    )

    beforeEach {
        clearAllMocks()
    }

    context("create") {

        test("일반 댓글을 저장하고 생성된 ID를 반환한다") {
            // Given
            val newComment = NewCommentFixture.create()
            val post = PostFixture.create(id = newComment.postId)
            val expectedCommentId = 1L

            every { postQueryRepository.findOne(newComment.postId) } returns post
            every { commentCommandRepository.save(any()) } returns expectedCommentId
            every { postCommandRepository.incrementComments(newComment.postId) } returns Unit

            // When
            val result = service.create(newComment)

            // Then
            result shouldBe expectedCommentId
            verify { commentCommandRepository.save(newComment) }
            verify { postCommandRepository.incrementComments(newComment.postId) }
        }

        test("대댓글을 저장하고 생성된 ID를 반환한다") {
            // Given
            val parentComment = CommentFixture.create(id = 10L, parentCommentId = null)
            val newComment = NewCommentFixture.create(parentCommentId = parentComment.id)
            val post = PostFixture.create(id = newComment.postId)
            val expectedCommentId = 2L

            every { postQueryRepository.findOne(newComment.postId) } returns post
            every { commentQueryRepository.findOne(parentComment.id) } returns parentComment
            every { commentCommandRepository.save(any()) } returns expectedCommentId
            every { postCommandRepository.incrementComments(newComment.postId) } returns Unit

            // When
            val result = service.create(newComment)

            // Then
            result shouldBe expectedCommentId
            verify { commentCommandRepository.save(newComment) }
            verify { postCommandRepository.incrementComments(newComment.postId) }
        }

        test("부모 댓글이 대댓글이면 ReplyDepthExceededException이 발생한다") {
            // Given
            val parentComment = CommentFixture.create(id = 10L, parentCommentId = 5L)
            val newComment = NewCommentFixture.create(parentCommentId = parentComment.id)
            val post = PostFixture.create(id = newComment.postId)

            every { postQueryRepository.findOne(newComment.postId) } returns post
            every { commentQueryRepository.findOne(parentComment.id) } returns parentComment

            // When & Then
            shouldThrow<ReplyDepthExceededException> {
                service.create(newComment)
            }.message shouldBe "대댓글에는 답글을 달 수 없습니다."
        }

        test("존재하지 않는 게시글이면 EntityNotFoundException이 발생한다") {
            // Given
            val newComment = NewCommentFixture.create(postId = 999L)

            every { postQueryRepository.findOne(newComment.postId) } throws
                EntityNotFoundException(
                    entityType = com.konkuk.ma.exception.EntityType.COMMUNITY_POST,
                    value = newComment.postId.toString(),
                )

            // When & Then
            shouldThrow<EntityNotFoundException> {
                service.create(newComment)
            }.message shouldBe "CommunityPost을(를) 찾을 수 없습니다."
        }
    }
})
