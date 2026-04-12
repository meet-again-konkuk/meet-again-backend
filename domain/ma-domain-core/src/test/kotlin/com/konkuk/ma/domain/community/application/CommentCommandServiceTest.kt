package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.CommentValidator
import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.exception.CommentAccessDeniedException
import com.konkuk.ma.domain.community.exception.PostNotFoundException
import com.konkuk.ma.domain.community.fixture.CommentFixture
import com.konkuk.ma.domain.community.fixture.NewCommentFixture
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class CommentCommandServiceTest : FunSpec({

    val commentCommandRepository = mockk<CommentCommandRepository>()
    val commentQueryRepository = mockk<CommentQueryRepository>()
    val commentValidator = mockk<CommentValidator>()
    val service = CommentCommandService(
        commentCommandRepository,
        commentQueryRepository,
        commentValidator,
    )

    beforeEach {
        clearAllMocks()
    }

    context("create") {

        test("댓글을 저장하고 생성된 ID를 반환한다") {
            // Given
            val newComment = NewCommentFixture.create()
            val expectedCommentId = 1L

            every { commentValidator.validate(newComment) } just runs
            every { commentCommandRepository.save(any()) } returns expectedCommentId

            // When
            val result = service.create(newComment)

            // Then
            result shouldBe expectedCommentId
            verify { commentValidator.validate(newComment) }
            verify { commentCommandRepository.save(newComment) }
        }

        test("검증 실패 시 예외가 전파된다") {
            // Given
            val newComment = NewCommentFixture.create(postId = 999L)

            every { commentValidator.validate(newComment) } throws
                PostNotFoundException(newComment.postId)

            // When & Then
            shouldThrow<PostNotFoundException> {
                service.create(newComment)
            }.message shouldBe "존재하지 않는 게시글에는 댓글을 달 수 없습니다."
        }
    }

    context("delete") {

        test("댓글을 삭제한다") {
            // Given
            val comment = CommentFixture.create()

            every { commentQueryRepository.findOne(comment.id) } returns comment
            every { commentCommandRepository.delete(comment.id) } just runs

            // When
            service.delete(comment.id, comment.authorEmail.value)

            // Then
            verify { commentCommandRepository.delete(comment.id) }
        }

        test("소유권이 없는 댓글을 삭제하면 CommentAccessDeniedException이 발생한다") {
            // Given
            val comment = CommentFixture.create()
            val otherEmail = "other@example.com"

            every { commentQueryRepository.findOne(comment.id) } returns comment

            // When & Then
            shouldThrow<CommentAccessDeniedException> {
                service.delete(comment.id, otherEmail)
            }.message shouldBe "댓글에 대한 접근 권한이 없습니다."
        }

        test("존재하지 않는 댓글을 삭제하면 EntityNotFoundException이 발생한다") {
            // Given
            val nonExistentId = 999L

            every { commentQueryRepository.findOne(nonExistentId) } throws
                EntityNotFoundException(EntityType.COMMUNITY_COMMENT, nonExistentId.toString())

            // When & Then
            shouldThrow<EntityNotFoundException> {
                service.delete(nonExistentId, "any@example.com")
            }.message shouldBe "CommunityComment을(를) 찾을 수 없습니다."
        }
    }
})
