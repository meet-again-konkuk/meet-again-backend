package com.konkuk.ma.domain.community.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.application.PostQueryService
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.PhoneNumber
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.requestParam
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.support.security.WithAuthMember
import com.konkuk.ma.vocabulary.categoryParam
import com.konkuk.ma.vocabulary.cursorIdParam
import com.konkuk.ma.vocabulary.postCategory
import com.konkuk.ma.vocabulary.postComments
import com.konkuk.ma.vocabulary.postContent
import com.konkuk.ma.vocabulary.postId
import com.konkuk.ma.vocabulary.postLikes
import com.konkuk.ma.vocabulary.postNickname
import com.konkuk.ma.vocabulary.postTimeAgo
import com.konkuk.ma.vocabulary.postTitle
import com.konkuk.ma.vocabulary.postsHasNext
import com.konkuk.ma.vocabulary.postsNextCursorId
import com.konkuk.ma.vocabulary.sizeParam
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import java.time.LocalDateTime

@WebMvcTest(CommunityPostQueryApi::class)
@BaseApiTest
@WithAuthMember(email = "test@example.com")
class CommunityPostQueryApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val postQueryService: PostQueryService,
    @MockkBean private val memberQueryRepository: MemberQueryRepository,
) : FunSpec({

    test("커뮤니티 게시글 목록 조회 API 문서화") {
        // Given
        val authorEmail = "author@example.com"
        val post = Post(
            id = 1L,
            authorEmail = authorEmail,
            category = PostCategory.CHEER,
            title = "안녕하세요",
            content = "반갑습니다",
            likes = 5,
            comments = 3,
            createdDate = LocalDateTime.now().minusMinutes(5),
        )
        val cursorResult = CursorResult(
            data = listOf(post),
            hasNext = true,
            nextCursorId = 1L,
        )
        val member = Member(
            email = authorEmail,
            password = "password",
            nickname = "테스트닉네임",
            gender = Gender.MALE,
            phoneNumber = PhoneNumber("01012345678"),
            name = "홍길동",
            region = Region.SEOUL,
            birthDate = java.time.LocalDate.of(1999, 12, 31),
            highSchool = null,
            university = null,
        )

        every { postQueryService.find(PostCategory.CHEER, CursorIdCondition(null, 20)) } returns cursorResult
        every { memberQueryRepository.findByEmails(setOf(authorEmail)) } returns listOf(member)

        // When & Then
        mockMvc.getJson("/api/community/posts") {
            param("category", "CHEER")
            param("size", "20")
        }
            .andExpect { status { isOk() } }
            .andDocument(
                "community/find-posts",
                requestParam(
                    categoryParam(),
                    cursorIdParam(),
                    sizeParam(),
                ),
                responseBody(
                    postId(),
                    postNickname(),
                    postCategory(),
                    postTitle(),
                    postContent(),
                    postLikes(),
                    postComments(),
                    postTimeAgo(),
                    postsHasNext(),
                    postsNextCursorId(),
                ),
            )
    }

    test("게시글이 없는 경우 빈 목록 반환") {
        // Given
        val cursorResult: CursorResult<List<Post>> = CursorResult(
            data = emptyList(),
            hasNext = false,
            nextCursorId = null,
        )

        every { postQueryService.find(PostCategory.SUCCESS_STORY, CursorIdCondition(null, 20)) } returns cursorResult

        // When & Then
        mockMvc.getJson("/api/community/posts") {
            param("category", "SUCCESS_STORY")
        }
            .andExpect { status { isOk() } }
            .andDocument(
                "community/find-posts-empty",
            )
    }
})
