package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.community.fixture.PostFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class PostQueryServiceTest : FunSpec({

    val postQueryRepository = mockk<PostQueryRepository>()
    val memberQueryRepository = mockk<MemberQueryRepository>()
    val service = PostQueryService(postQueryRepository, memberQueryRepository)

    beforeEach {
        clearAllMocks()
    }

    context("find") {

        test("카테고리와 커서로 게시글 목록을 조회하고 닉네임을 매핑한다") {
            // Given
            val category = PostCategory.SUCCESS_STORY
            val cursorCondition = CursorIdCondition(cursorId = null, size = 20)
            val authorEmail = "author@example.com"
            val post = PostFixture.create(category = category, authorEmail = authorEmail)
            val cursorResult = CursorResult(
                data = listOf(post),
                hasNext = false,
                nextCursorId = null,
            )
            val member = MemberFixture.create(email = authorEmail, nickname = "테스트닉네임")

            every { postQueryRepository.find(category, cursorCondition) } returns cursorResult
            every { memberQueryRepository.findByEmails(setOf(authorEmail)) } returns listOf(member)

            // When
            val result = service.find(category, cursorCondition)

            // Then
            result.data shouldHaveSize 1
            result.data[0].post.category shouldBe category
            result.data[0].nickname shouldBe "테스트닉네임"
            result.hasNext shouldBe false
            result.nextCursorId shouldBe null
        }

        test("게시글이 없으면 빈 목록을 반환한다") {
            // Given
            val category = PostCategory.CHEER
            val cursorCondition = CursorIdCondition(cursorId = null, size = 20)
            val cursorResult: CursorResult<List<Post>> = CursorResult(
                data = emptyList(),
                hasNext = false,
                nextCursorId = null,
            )

            every { postQueryRepository.find(category, cursorCondition) } returns cursorResult
            every { memberQueryRepository.findByEmails(emptySet()) } returns emptyList()

            // When
            val result = service.find(category, cursorCondition)

            // Then
            result.data shouldHaveSize 0
            result.hasNext shouldBe false
        }

        test("다음 페이지가 있으면 hasNext가 true이고 nextCursorId를 반환한다") {
            // Given
            val category = PostCategory.SUCCESS_STORY
            val cursorCondition = CursorIdCondition(cursorId = null, size = 20)
            val authorEmail = "author@example.com"
            val posts = List(20) { PostFixture.create(id = (20 - it).toLong(), category = category, authorEmail = authorEmail) }
            val cursorResult = CursorResult(
                data = posts,
                hasNext = true,
                nextCursorId = 1L,
            )
            val member = MemberFixture.create(email = authorEmail, nickname = "테스트닉네임")

            every { postQueryRepository.find(category, cursorCondition) } returns cursorResult
            every { memberQueryRepository.findByEmails(setOf(authorEmail)) } returns listOf(member)

            // When
            val result = service.find(category, cursorCondition)

            // Then
            result.data shouldHaveSize 20
            result.hasNext shouldBe true
            result.nextCursorId shouldBe 1L
        }

        test("닉네임을 찾을 수 없으면 '알 수 없음'으로 표시한다") {
            // Given
            val category = PostCategory.CHEER
            val cursorCondition = CursorIdCondition(cursorId = null, size = 20)
            val post = PostFixture.create(category = category, authorEmail = "unknown@example.com")
            val cursorResult = CursorResult(
                data = listOf(post),
                hasNext = false,
                nextCursorId = null,
            )

            every { postQueryRepository.find(category, cursorCondition) } returns cursorResult
            every { memberQueryRepository.findByEmails(setOf("unknown@example.com")) } returns emptyList()

            // When
            val result = service.find(category, cursorCondition)

            // Then
            result.data shouldHaveSize 1
            result.data[0].nickname shouldBe "알 수 없음"
        }
    }
})
