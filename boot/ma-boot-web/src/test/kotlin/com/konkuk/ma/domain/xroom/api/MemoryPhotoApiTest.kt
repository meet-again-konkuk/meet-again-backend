package com.konkuk.ma.domain.xroom.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.xroom.application.MemoryPhotoCommandService
import com.konkuk.ma.domain.xroom.application.result.MediaUploadResult
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.deleteJson
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.mediaId
import com.konkuk.ma.vocabulary.memoryId
import com.konkuk.ma.vocabulary.memoryIdPath
import com.konkuk.ma.vocabulary.photoDeleted
import com.konkuk.ma.vocabulary.photoUrl
import com.konkuk.ma.vocabulary.thumbnailUrl
import com.konkuk.ma.vocabulary.xroomIdPath
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

@WebMvcTest(MemoryPhotoApi::class)
@BaseApiTest
class MemoryPhotoApiTest(
    private val mockMvc: MockMvc,
    private val idObfuscator: IdObfuscator,
    @MockkBean private val memoryPhotoCommandService: MemoryPhotoCommandService,
) : FunSpec({

    val authMemberId = 1L

    test("기억 사진 업로드 API 문서화") {
        // Given
        val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, 1L)
        val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, 1L)
        val photo = MockMultipartFile(
            "photo",
            "memory-photo.jpg",
            "image/jpeg",
            "fake-image-content".toByteArray(),
        )
        every {
            memoryPhotoCommandService.uploadPhoto(1L, 1L, authMemberId, any<PhotoFile>())
        } returns MediaUploadResult(
            mediaId = 1L,
            storageKey = "xroom/1/memory/1/photo.jpg",
            thumbnailKey = "xroom/1/memory/1/thumb.jpg",
        )

        // When & Then
        mockMvc.perform(
            multipart(
                "/api/xrooms/{xroomId}/memories/{memoryId}/photo",
                encodedXroomId,
                encodedMemoryId,
            ).file(photo),
        )
            .andExpect(status().isCreated)
            .andDocument(
                "xroom/upload-memory-photo",
                pathVariables(
                    xroomIdPath(),
                    memoryIdPath(),
                ),
                requestParts(
                    partWithName("photo").description("업로드할 사진 파일"),
                ),
                responseBody(
                    mediaId("mediaId"),
                    photoUrl("photoUrl"),
                    thumbnailUrl("thumbnailUrl") isOptional true,
                ),
            )
    }

    test("기억 사진 삭제 API 문서화") {
        // Given
        val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, 1L)
        val encodedMemoryId = idObfuscator.encode(ObfuscationType.MEMORY, 1L)
        every {
            memoryPhotoCommandService.removePhoto(1L, 1L, authMemberId)
        } just runs

        // When & Then
        mockMvc.deleteJson(
            "/api/xrooms/{xroomId}/memories/{memoryId}/photo",
            encodedXroomId,
            encodedMemoryId,
        )
            .andExpect { status { isOk() } }
            .andDocument(
                "xroom/delete-memory-photo",
                pathVariables(
                    xroomIdPath(),
                    memoryIdPath(),
                ),
                responseBody(
                    memoryId("memoryId"),
                    photoDeleted("photoDeleted"),
                ),
            )
    }
})
