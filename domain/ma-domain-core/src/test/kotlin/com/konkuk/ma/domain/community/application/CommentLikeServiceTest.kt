package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.CommentLikeResult
import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.fixture.CommentLikeFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class CommentLikeServiceTest : FunSpec({

    val commentLikeRepository = mockk<CommentLikeRepository>()
    val commentCommandRepository = mockk<CommentCommandRepository>()
    val commentQueryRepository = mockk<CommentQueryRepository>()
    val service = CommentLikeService(
        commentLikeRepository,
        commentCommandRepository,
        commentQueryRepository,
    )

    beforeEach {
        clearAllMocks()
    }

    context("toggle") {

        test("좋아요가 없는 상태에서 토글하면 좋아요를 추가하고 liked=true를 반환한다") {
            // Given
            val commentId = 1L
            val memberEmail = "user@example.com"
            val expectedLikeCount = 1

            every { commentLikeRepository.findOneOrNull(commentId, memberEmail) } returns null
            every { commentLikeRepository.save(any()) } returns 1L
            every { commentCommandRepository.increaseLikes(commentId) } just runs
            every { commentQueryRepository.findLikeCount(commentId) } returns expectedLikeCount

            // When
            val result = service.toggle(commentId, memberEmail)

            // Then
            result.liked.shouldBeTrue()
            result.likeCount shouldBe expectedLikeCount
            verify { commentLikeRepository.save(any()) }
            verify { commentCommandRepository.increaseLikes(commentId) }
        }

        test("좋아요가 있는 상태에서 토글하면 좋아요를 삭제하고 liked=false를 반환한다") {
            // Given
            val existingLike = CommentLikeFixture.create()
            val commentId = existingLike.commentId
            val memberEmail = existingLike.memberEmail
            val expectedLikeCount = 0

            every { commentLikeRepository.findOneOrNull(commentId, memberEmail) } returns existingLike
            every { commentLikeRepository.delete(commentId, memberEmail) } just runs
            every { commentCommandRepository.decreaseLikes(commentId) } just runs
            every { commentQueryRepository.findLikeCount(commentId) } returns expectedLikeCount

            // When
            val result = service.toggle(commentId, memberEmail)

            // Then
            result.liked.shouldBeFalse()
            result.likeCount shouldBe expectedLikeCount
            verify { commentLikeRepository.delete(commentId, memberEmail) }
            verify { commentCommandRepository.decreaseLikes(commentId) }
        }

        test("좋아요 추가 시 save와 increaseLikes가 호출되고 delete와 decreaseLikes는 호출되지 않는다") {
            // Given
            val commentId = 1L
            val memberEmail = "user@example.com"

            every { commentLikeRepository.findOneOrNull(commentId, memberEmail) } returns null
            every { commentLikeRepository.save(any()) } returns 1L
            every { commentCommandRepository.increaseLikes(commentId) } just runs
            every { commentQueryRepository.findLikeCount(commentId) } returns 1

            // When
            service.toggle(commentId, memberEmail)

            // Then
            verify(exactly = 1) { commentLikeRepository.save(any()) }
            verify(exactly = 1) { commentCommandRepository.increaseLikes(commentId) }
            verify(exactly = 0) { commentLikeRepository.delete(any(), any()) }
            verify(exactly = 0) { commentCommandRepository.decreaseLikes(any()) }
        }

        test("좋아요 삭제 시 delete와 decreaseLikes가 호출되고 save와 increaseLikes는 호출되지 않는다") {
            // Given
            val existingLike = CommentLikeFixture.create()
            val commentId = existingLike.commentId
            val memberEmail = existingLike.memberEmail

            every { commentLikeRepository.findOneOrNull(commentId, memberEmail) } returns existingLike
            every { commentLikeRepository.delete(commentId, memberEmail) } just runs
            every { commentCommandRepository.decreaseLikes(commentId) } just runs
            every { commentQueryRepository.findLikeCount(commentId) } returns 0

            // When
            service.toggle(commentId, memberEmail)

            // Then
            verify(exactly = 1) { commentLikeRepository.delete(commentId, memberEmail) }
            verify(exactly = 1) { commentCommandRepository.decreaseLikes(commentId) }
            verify(exactly = 0) { commentLikeRepository.save(any()) }
            verify(exactly = 0) { commentCommandRepository.increaseLikes(any()) }
        }
    }
})
