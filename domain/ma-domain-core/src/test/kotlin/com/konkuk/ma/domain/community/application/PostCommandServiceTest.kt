package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import com.konkuk.ma.domain.community.fixture.NewPostFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PostCommandServiceTest : FunSpec({

    val postCommandRepository = mockk<PostCommandRepository>()
    val service = PostCommandService(postCommandRepository)

    beforeEach {
        clearAllMocks()
    }

    context("create") {

        test("게시글을 저장하고 생성된 ID를 반환한다") {
            // Given
            val newPost = NewPostFixture.create(
                authorEmail = "test@example.com",
                category = PostCategory.SUCCESS_STORY,
                title = "테스트 게시글",
                content = "테스트 내용입니다.",
            )
            val expectedPostId = 1L

            every { postCommandRepository.save(any()) } returns expectedPostId

            // When
            val result = service.create(newPost)

            // Then
            result shouldBe expectedPostId
            verify { postCommandRepository.save(match {
                it.authorEmail == newPost.authorEmail &&
                    it.category == newPost.category &&
                    it.title == newPost.title &&
                    it.content == newPost.content
            }) }
        }
    }
})
