package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.common.domain.page.PageResult
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

        test("카테고리와 페이지로 게시글 목록을 조회한다") {
            // Given
            val category = PostCategory.SUCCESS_STORY
            val page = 0
            val post = PostFixture.create(category = category)
            val pageResult = PageResult(
                data = Posts(listOf(post)),
                totalCount = 1L,
                currentPage = page,
                pageSize = Posts.PAGE_SIZE,
            )

            every { postQueryRepository.find(category, page) } returns pageResult

            // When
            val result = service.find(category, page)

            // Then
            result.data.data shouldHaveSize 1
            result.data.data[0].category shouldBe category
            result.totalCount shouldBe 1L
            result.currentPage shouldBe page
        }

        test("게시글이 없으면 빈 목록을 반환한다") {
            // Given
            val category = PostCategory.CHEER
            val page = 0
            val pageResult = PageResult(
                data = Posts(emptyList()),
                totalCount = 0L,
                currentPage = page,
                pageSize = Posts.PAGE_SIZE,
            )

            every { postQueryRepository.find(category, page) } returns pageResult

            // When
            val result = service.find(category, page)

            // Then
            result.data.data shouldHaveSize 0
            result.totalCount shouldBe 0L
        }
    }
})
