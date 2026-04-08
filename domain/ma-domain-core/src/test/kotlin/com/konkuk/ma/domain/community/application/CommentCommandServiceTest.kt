package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.CommentValidator
import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
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

    val postCommandRepository = mockk<PostCommandRepository>()
    val commentCommandRepository = mockk<CommentCommandRepository>()
    val commentValidator = mockk<CommentValidator>()
    val service = CommentCommandService(
        postCommandRepository,
        commentCommandRepository,
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
            every { postCommandRepository.incrementComments(newComment.postId) } returns Unit

            // When
            val result = service.create(newComment)

            // Then
            result shouldBe expectedCommentId
            verify { commentValidator.validate(newComment) }
            verify { commentCommandRepository.save(newComment) }
            verify { postCommandRepository.incrementComments(newComment.postId) }
        }

        test("검증 실패 시 예외가 전파된다") {
            // Given
            val newComment = NewCommentFixture.create(postId = 999L)

            every { commentValidator.validate(newComment) } throws
                EntityNotFoundException(EntityType.COMMUNITY_POST, newComment.postId.toString())

            // When & Then
            shouldThrow<EntityNotFoundException> {
                service.create(newComment)
            }.message shouldBe "CommunityPost을(를) 찾을 수 없습니다."
        }
    }
})
