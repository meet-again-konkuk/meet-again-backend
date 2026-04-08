package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
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

class PostLikeServiceTest : FunSpec({

    val postLikeRepository = mockk<PostLikeRepository>()
    val postCommandRepository = mockk<PostCommandRepository>()
    val service = PostLikeService(postLikeRepository, postCommandRepository)

    beforeEach {
        clearAllMocks()
    }

    context("like") {

        test("좋아요를 추가하고 liked=true와 likeCount를 반환한다") {
            // Given
            val postId = 1L
            val memberEmail = "user@example.com"

            every { postLikeRepository.save(any()) } returns 1L
            every { postCommandRepository.increaseLikes(postId) } returns 1

            // When
            val result = service.like(postId, memberEmail)

            // Then
            result.liked.shouldBeTrue()
            result.likeCount shouldBe 1
            verify { postLikeRepository.save(any()) }
            verify { postCommandRepository.increaseLikes(postId) }
        }
    }

    context("unlike") {

        test("좋아요를 삭제하고 liked=false와 likeCount를 반환한다") {
            // Given
            val postId = 1L
            val memberEmail = "user@example.com"

            every { postLikeRepository.delete(postId, memberEmail) } just runs
            every { postCommandRepository.decreaseLikes(postId) } returns 0

            // When
            val result = service.unlike(postId, memberEmail)

            // Then
            result.liked.shouldBeFalse()
            result.likeCount shouldBe 0
            verify { postLikeRepository.delete(postId, memberEmail) }
            verify { postCommandRepository.decreaseLikes(postId) }
        }
    }
})
