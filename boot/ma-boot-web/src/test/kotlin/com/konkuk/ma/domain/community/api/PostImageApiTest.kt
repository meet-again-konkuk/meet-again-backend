package com.konkuk.ma.domain.community.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.community.application.PostImageCommandService
import com.konkuk.ma.domain.community.domain.image.PostImageUploadResult
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.deleteJson
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.postIdPath
import com.konkuk.ma.vocabulary.postImageMediaId
import com.konkuk.ma.vocabulary.postImagePostId
import com.konkuk.ma.vocabulary.postImageUrl
import com.konkuk.ma.vocabulary.postThumbnailUrl
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart
import org.springframework.restdocs.request.RequestDocumentation.partWithName
import org.springframework.restdocs.request.RequestDocumentation.requestParts
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(PostImageApi::class)
@BaseApiTest
class PostImageApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val postImageCommandService: PostImageCommandService,
) : FunSpec({

    val authMemberId = 1L

    fun imagePart() = MockMultipartFile(
        "image",
        "post-image.jpg",
        "image/jpeg",
        "fake-image-content".toByteArray(),
    )

    test("커뮤니티 게시글 이미지 업로드 API 문서화") {
        // Given
        every {
            postImageCommandService.upload(1L, authMemberId, any<PhotoFile>())
        } returns PostImageUploadResult(
            mediaId = 10L,
            postId = 1L,
            imageUrl = "/files/community/post/1/image.jpg",
            thumbnailUrl = "/files/community/post/1/thumb.jpg",
        )

        // When & Then
        mockMvc.perform(
            multipart("/api/community/posts/{postId}/image", 1L).file(imagePart()),
        )
            .andExpect(status().isCreated())
            .andDocument(
                "community/upload-post-image",
                pathVariables(
                    postIdPath(),
                ),
                requestParts(
                    partWithName("image").description("업로드할 이미지 파일 (jpg, jpeg, png, svg, webp / 최대 10MB)"),
                ),
                responseBody(
                    postImageMediaId(),
                    postImagePostId(),
                    postImageUrl(),
                    postThumbnailUrl() isOptional true,
                ),
            )
    }

    test("지원하지 않는 확장자면 400을 반환한다") {
        // Given
        val unsupported = MockMultipartFile(
            "image",
            "malware.exe",
            "application/octet-stream",
            "fake-content".toByteArray(),
        )

        // When & Then
        mockMvc.perform(
            multipart("/api/community/posts/{postId}/image", 1L).file(unsupported),
        )
            .andExpect(status().isBadRequest())
    }

    test("소유권이 없는 게시글에 이미지 업로드 시 403을 반환한다") {
        // Given
        every {
            postImageCommandService.upload(any(), any(), any())
        } throws AccessDeniedException(EntityType.COMMUNITY_POST, "owner@example.com", "holeman@naver.com")

        // When & Then
        mockMvc.perform(
            multipart("/api/community/posts/{postId}/image", 1L).file(imagePart()),
        )
            .andExpect(status().isForbidden())
    }

    test("존재하지 않는 게시글에 이미지 업로드 시 404를 반환한다") {
        // Given
        every {
            postImageCommandService.upload(any(), any(), any())
        } throws EntityNotFoundException(EntityType.COMMUNITY_POST, "999")

        // When & Then
        mockMvc.perform(
            multipart("/api/community/posts/{postId}/image", 999L).file(imagePart()),
        )
            .andExpect(status().isNotFound())
    }

    test("커뮤니티 게시글 이미지 삭제 API 문서화") {
        // Given
        every { postImageCommandService.delete(1L, authMemberId) } just runs

        // When & Then
        mockMvc.deleteJson("/api/community/posts/{postId}/image", 1L)
            .andExpect { status { isNoContent() } }
            .andDocument(
                "community/delete-post-image",
                pathVariables(
                    postIdPath(),
                ),
            )
    }

    test("소유권이 없는 게시글 이미지 삭제 시 403을 반환한다") {
        // Given
        every {
            postImageCommandService.delete(any(), any())
        } throws AccessDeniedException(EntityType.COMMUNITY_POST, "owner@example.com", "holeman@naver.com")

        // When & Then
        mockMvc.deleteJson("/api/community/posts/{postId}/image", 1L)
            .andExpect { status { isForbidden() } }
    }
})
