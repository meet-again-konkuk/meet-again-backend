package com.konkuk.ma.domain.community.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.application.PostCommandService
import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.newPostCategory
import com.konkuk.ma.vocabulary.newPostContent
import com.konkuk.ma.vocabulary.newPostId
import com.konkuk.ma.vocabulary.newPostTitle
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(PostCommandApi::class)
@BaseApiTest
class PostCommandApiTest(
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
                it.authorId == 1L &&
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

    test("제목이 최대 글자수를 초과하면 400을 반환한다") {
        // Given
        val request = mapOf(
            "category" to "CHEER",
            "title" to "가".repeat(NewPost.MAX_TITLE_LENGTH + 1),
            "content" to "내용",
        )

        // When & Then
        mockMvc.postJson("/api/community/posts") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isBadRequest() } }
    }

    test("내용이 최대 글자수를 초과하면 400을 반환한다") {
        // Given
        val request = mapOf(
            "category" to "CHEER",
            "title" to "제목",
            "content" to "가".repeat(NewPost.MAX_CONTENT_LENGTH + 1),
        )

        // When & Then
        mockMvc.postJson("/api/community/posts") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isBadRequest() } }
    }

    test("제목이 비어있으면 400을 반환한다") {
        // Given
        val request = mapOf(
            "category" to "CHEER",
            "title" to "",
            "content" to "내용",
        )

        // When & Then
        mockMvc.postJson("/api/community/posts") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isBadRequest() } }
    }

    test("내용이 비어있으면 400을 반환한다") {
        // Given
        val request = mapOf(
            "category" to "CHEER",
            "title" to "제목",
            "content" to "",
        )

        // When & Then
        mockMvc.postJson("/api/community/posts") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isBadRequest() } }
    }
})
