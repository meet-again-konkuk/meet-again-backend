package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.common.domain.page.CursorRequest
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.Posts
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.community.fixture.PostFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class PostQueryServiceTest : FunSpec({

    val postQueryRepository = mockk<PostQueryRepository>()
    val service = PostQueryService(postQueryRepository)

    beforeEach {
        clearAllMocks()
    }

    context("find") {

        test("카테고리와 커서로 게시글 목록을 조회한다") {
            // Given
            val category = PostCategory.SUCCESS_STORY
            val cursorRequest = CursorRequest(cursorId = null, size = 20)
            val post = PostFixture.create(category = category)
            val cursorResult = CursorResult(
                data = Posts(listOf(post)),
                hasNext = false,
                nextCursorId = null,
            )

            every { postQueryRepository.find(category, cursorRequest) } returns cursorResult

            // When
            val result = service.find(category, cursorRequest)

            // Then
            result.data.data shouldHaveSize 1
            result.data.data[0].category shouldBe category
            result.hasNext shouldBe false
            result.nextCursorId shouldBe null
        }

        test("게시글이 없으면 빈 목록을 반환한다") {
            // Given
            val category = PostCategory.CHEER
            val cursorRequest = CursorRequest(cursorId = null, size = 20)
            val cursorResult = CursorResult(
                data = Posts(emptyList()),
                hasNext = false,
                nextCursorId = null,
            )

            every { postQueryRepository.find(category, cursorRequest) } returns cursorResult

            // When
            val result = service.find(category, cursorRequest)

            // Then
            result.data.data shouldHaveSize 0
            result.hasNext shouldBe false
        }

        test("다음 페이지가 있으면 hasNext가 true이고 nextCursorId를 반환한다") {
            // Given
            val category = PostCategory.SUCCESS_STORY
            val cursorRequest = CursorRequest(cursorId = null, size = 20)
            val posts = List(20) { PostFixture.create(id = (20 - it).toLong(), category = category) }
            val cursorResult = CursorResult(
                data = Posts(posts),
                hasNext = true,
                nextCursorId = 1L,
            )

            every { postQueryRepository.find(category, cursorRequest) } returns cursorResult

            // When
            val result = service.find(category, cursorRequest)

            // Then
            result.data.data shouldHaveSize 20
            result.hasNext shouldBe true
            result.nextCursorId shouldBe 1L
        }
    }
})
