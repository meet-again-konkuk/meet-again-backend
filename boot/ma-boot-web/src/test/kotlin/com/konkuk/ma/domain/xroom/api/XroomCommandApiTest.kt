package com.konkuk.ma.domain.xroom.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.xroom.application.XroomCommandService
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.patchJson
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.finalMessage
import com.konkuk.ma.vocabulary.targetInfoId
import com.konkuk.ma.vocabulary.xroomId
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(XroomCommandApi::class)
@BaseApiTest
class XroomCommandApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    private val idObfuscator: IdObfuscator,
    @MockkBean private val xroomCommandService: XroomCommandService,
) : FunSpec({

    val authMemberId = 1L

    test("X룸 생성 API 문서화") {
        // Given
        val encryptedTargetInfoId = idObfuscator.encode(ObfuscationType.TARGET_INFO, 1L)
        val encryptedXroomId = idObfuscator.encode(ObfuscationType.XROOM, 1L)
        val request = mapOf(
            "targetInfoId" to encryptedTargetInfoId,
            "finalMessage" to "고마웠어",
        )
        every { xroomCommandService.create(1L, authMemberId, "고마웠어") } returns 1L

        // When & Then
        mockMvc.postJson("/api/xrooms") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect {
                status { isCreated() }
            }
            .andDocument(
                "xroom/create-xroom",
                requestBody(
                    targetInfoId(),
                    finalMessage() means "마지막으로 전하는 메시지 (선택)",
                ),
                responseBody(
                    xroomId(),
                ),
            )
    }

    test("X룸 마지막 메시지 수정 API 문서화") {
        // Given
        val encryptedXroomId = idObfuscator.encode(ObfuscationType.XROOM, 1L)
        val request = mapOf(
            "finalMessage" to "잘 지내",
        )
        every { xroomCommandService.updateFinalMessage(1L, authMemberId, "잘 지내") } returns 1L

        // When & Then
        mockMvc.patchJson("/api/xrooms/$encryptedXroomId") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect {
                status { isOk() }
            }
            .andDocument(
                "xroom/update-final-message",
                requestBody(
                    finalMessage() means "마지막으로 전하는 메시지 (선택, null이면 삭제)",
                ),
                responseBody(
                    xroomId(),
                ),
            )
    }
})
