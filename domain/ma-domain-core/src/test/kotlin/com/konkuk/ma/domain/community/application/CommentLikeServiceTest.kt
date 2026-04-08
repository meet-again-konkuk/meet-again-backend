package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
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
    val service = CommentLikeService(commentLikeRepository, commentCommandRepository)

    beforeEach {
        clearAllMocks()
    }

    context("like") {

        test("좋아요를 추가하고 liked=true와 likeCount를 반환한다") {
            // Given
            val commentId = 1L
            val memberEmail = "user@example.com"

            every { commentLikeRepository.save(any()) } returns 1L
            every { commentCommandRepository.increaseLikes(commentId) } returns 1

            // When
            val result = service.like(commentId, memberEmail)

            // Then
            result.liked.shouldBeTrue()
            result.likeCount shouldBe 1
            verify { commentLikeRepository.save(any()) }
            verify { commentCommandRepository.increaseLikes(commentId) }
        }
    }

    context("unlike") {

        test("좋아요를 삭제하고 liked=false와 likeCount를 반환한다") {
            // Given
            val commentId = 1L
            val memberEmail = "user@example.com"

            every { commentLikeRepository.delete(commentId, memberEmail) } just runs
            every { commentCommandRepository.decreaseLikes(commentId) } returns 0

            // When
            val result = service.unlike(commentId, memberEmail)

            // Then
            result.liked.shouldBeFalse()
            result.likeCount shouldBe 0
            verify { commentLikeRepository.delete(commentId, memberEmail) }
            verify { commentCommandRepository.decreaseLikes(commentId) }
        }
    }
})
