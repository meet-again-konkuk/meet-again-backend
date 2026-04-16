package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.community.fixture.CommentFixture
import com.konkuk.ma.domain.community.fixture.NewCommentFixture
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.InvalidStateException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class CommentValidatorTest : FunSpec({

    val postQueryRepository = mockk<PostQueryRepository>()
    val commentQueryRepository = mockk<CommentQueryRepository>()
    val commentValidator = CommentValidator(postQueryRepository, commentQueryRepository)

    context("validate") {

        test("게시글이 존재하고 부모 댓글이 없으면 예외 없이 통과한다") {
            // Given
            val newComment = NewCommentFixture.create()

            every { postQueryRepository.exists(newComment.postId) } returns true

            // When & Then
            commentValidator.validate(newComment)
        }

        test("게시글이 존재하고 부모 댓글이 루트 댓글이면 예외 없이 통과한다") {
            // Given
            val parentCommentId = 10L
            val newComment = NewCommentFixture.create(parentCommentId = parentCommentId)
            val parentComment = CommentFixture.create(id = parentCommentId, parentCommentId = null)

            every { postQueryRepository.exists(newComment.postId) } returns true
            every { commentQueryRepository.findOne(parentCommentId) } returns parentComment

            // When & Then
            commentValidator.validate(newComment)
        }

        test("게시글이 존재하지 않으면 EntityNotFoundException이 발생한다") {
            // Given
            val newComment = NewCommentFixture.create()

            every { postQueryRepository.exists(newComment.postId) } returns false

            // When & Then
            shouldThrow<EntityNotFoundException> {
                commentValidator.validate(newComment)
            }.message shouldBe "CommunityPost을(를) 찾을 수 없습니다."
        }

        test("부모 댓글이 루트 댓글이 아니면 InvalidStateException이 발생한다") {
            // Given
            val parentCommentId = 10L
            val newComment = NewCommentFixture.create(parentCommentId = parentCommentId)
            val nonRootParent = CommentFixture.create(id = parentCommentId, parentCommentId = 5L)

            every { postQueryRepository.exists(newComment.postId) } returns true
            every { commentQueryRepository.findOne(parentCommentId) } returns nonRootParent

            // When & Then
            shouldThrow<InvalidStateException> {
                commentValidator.validate(newComment)
            }.message shouldBe "허용되지 않는 상태입니다."
        }
    }
})
