package com.konkuk.ma.domain.community.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.community.application.PostCommandService
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.support.security.WithAuthMember
import com.konkuk.ma.vocabulary.newPostCategory
import com.konkuk.ma.vocabulary.newPostContent
import com.konkuk.ma.vocabulary.newPostId
import com.konkuk.ma.vocabulary.newPostTitle
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(CommunityPostCommandApi::class)
@BaseApiTest
@WithAuthMember(email = "test@example.com")
class CommunityPostCommandApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val postCommandService: PostCommandService,
) : FunSpec({

    test("커뮤니티 게시글 작성 API 문서화") {
        // Given
        val request = mapOf(
            "category" to "CHEER",
            "title" to "안녕하세요",
            "content" to "반갑습니다",
        )

        every {
            postCommandService.create(match {
                it.authorEmail == "test@example.com" &&
                    it.category == PostCategory.CHEER &&
                    it.title == "안녕하세요" &&
                    it.content == "반갑습니다"
            })
        } returns 1L

        // When & Then
        mockMvc.postJson("/api/community/posts") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isCreated() } }
            .andDocument(
                "community/create-post",
                requestBody(
                    newPostCategory(),
                    newPostTitle(),
                    newPostContent(),
                ),
                responseBody(
                    newPostId(),
                ),
            )
    }
})
