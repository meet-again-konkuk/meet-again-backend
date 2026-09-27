package com.konkuk.ma.domain.member.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.member.application.MemberPhotoService
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.requestPart
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.message
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.request.RequestDocumentation.partWithName
import org.springframework.restdocs.request.RequestDocumentation.requestParts
import com.konkuk.ma.extension.deleteJson
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(MemberPhotoApi::class)
@BaseApiTest
class MemberPhotoApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val memberPhotoService: MemberPhotoService
) : FunSpec({

    test("사진 업로드 - multipart 파일 전송시 성공한다") {
        // Given
        val photoFile = MockMultipartFile(
            "photo",
            "profile.jpg",
            "image/jpeg",
            "fake-image-content".toByteArray()
        )

        every { memberPhotoService.upload(any(), any()) } just runs

        // When & Then
        mockMvc.perform(
            multipart("/api/members/photos")
                .file(photoFile)
        )
            .andExpect(status().isCreated)
            .andDocument(
                "member-photo-upload",
                requestParts(
                    partWithName("photo").description("프로필 사진 파일")
                ),
                responseBody(
                    message() means "업로드 결과 메시지",
                )
            )
    }

    test("사진 업로드 - 허용되지 않은 확장자이면 400을 반환한다") {
        // Given
        val invalidFile = MockMultipartFile(
            "photo",
            "profile.gif",
            "image/gif",
            "fake-image-content".toByteArray()
        )

        // When & Then
        mockMvc.perform(
            multipart("/api/members/photos")
                .file(invalidFile)
        )
            .andExpect(status().isBadRequest)
    }

    test("사진 삭제 - DELETE 요청시 성공한다") {
        // Given
        every { memberPhotoService.delete(any()) } just runs

        // When & Then
        mockMvc.deleteJson("/api/members/photos")
            .andExpect { status { isOk() } }
            .andDocument(
                "member-photo-delete",
                responseBody(
                    message() means "삭제 결과 메시지",
                )
            )
    }
})
