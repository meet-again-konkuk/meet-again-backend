package com.konkuk.ma.domain.xroom.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.xroom.application.XroomCommandService
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.requestParam
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.targetInfoIdParam
import com.konkuk.ma.vocabulary.xroomId
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl

@WebMvcTest(XroomCommandApi::class)
@BaseApiTest
class XroomCommandApiTest(
    private val mockMvc: MockMvc,
    private val idObfuscator: IdObfuscator,
    @MockkBean private val xroomCommandService: XroomCommandService,
) : FunSpec({

    val authMemberId = 1L

    test("X룸 생성 API 문서화") {
        // Given
        val encryptedTargetInfoId = idObfuscator.encode(ObfuscationType.TARGET_INFO, 1L)
        every { xroomCommandService.create(1L, authMemberId) } returns 1L

        // When & Then
        @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
        ResultActionsDsl(
            mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/xrooms?targetInfoId={targetInfoId}", encryptedTargetInfoId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            ),
            mockMvc
        )
            .andExpect { status { isCreated() } }
            .andDocument(
                "xroom/create-xroom",
                requestParam(
                    targetInfoIdParam(),
                ),
                responseBody(
                    xroomId(),
                ),
            )
    }
})
