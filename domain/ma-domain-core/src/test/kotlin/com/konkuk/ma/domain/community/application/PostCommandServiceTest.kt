package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PostCommandServiceTest : FunSpec({

    val postCommandRepository = mockk<PostCommandRepository>()
    val memberQueryRepository = mockk<MemberQueryRepository>()
    val service = PostCommandService(postCommandRepository, memberQueryRepository)

    beforeEach {
        clearAllMocks()
    }

    context("create") {

        test("회원 조회 후 게시글을 저장하고 생성된 ID를 반환한다") {
            // Given
            val member = MemberFixture.create()
            val category = PostCategory.SUCCESS_STORY
            val title = "테스트 게시글"
            val content = "테스트 내용입니다."
            val expectedPostId = 1L

            every { memberQueryRepository.findOne(member.email) } returns member
            every { postCommandRepository.save(any()) } returns expectedPostId

            // When
            val result = service.create(
                email = member.email,
                category = category,
                title = title,
                content = content,
            )

            // Then
            result shouldBe expectedPostId
            verify { memberQueryRepository.findOne(member.email) }
            verify { postCommandRepository.save(match {
                it.authorEmail == member.email &&
                    it.authorNickname == member.nickname &&
                    it.category == category &&
                    it.title == title &&
                    it.content == content
            }) }
        }

        test("조회한 회원의 닉네임을 게시글 작성자 닉네임으로 설정한다") {
            // Given
            val nickname = "커스텀닉네임"
            val member = MemberFixture.create(nickname = nickname)
            val expectedPostId = 2L

            every { memberQueryRepository.findOne(member.email) } returns member
            every { postCommandRepository.save(any()) } returns expectedPostId

            // When
            service.create(
                email = member.email,
                category = PostCategory.CHEER,
                title = "제목",
                content = "내용",
            )

            // Then
            verify { postCommandRepository.save(match {
                it.authorNickname == nickname
            }) }
        }
    }
})
