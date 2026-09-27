package com.konkuk.ma.domain.xroom.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.xroom.application.XroomCommandService
import com.konkuk.ma.domain.xroom.domain.CreatedXroom
import com.konkuk.ma.domain.xroom.domain.memory.NewMemories
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.patchJson
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.emotionTags
import com.konkuk.ma.vocabulary.eventDate
import com.konkuk.ma.vocabulary.eventDatePrecision
import com.konkuk.ma.vocabulary.finalMessage
import com.konkuk.ma.vocabulary.location
import com.konkuk.ma.vocabulary.memoryIds
import com.konkuk.ma.vocabulary.memoryLetter
import com.konkuk.ma.vocabulary.memoryText
import com.konkuk.ma.vocabulary.memoryTitle
import com.konkuk.ma.vocabulary.newMemories
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

    test("X룸 기억 일괄 생성 API 문서화") {
        // Given
        val encryptedTargetInfoId = idObfuscator.encode(ObfuscationType.TARGET_INFO, 1L)
        val request = mapOf(
            "targetInfoId" to encryptedTargetInfoId,
            "finalMessage" to "고마웠어",
            "memories" to listOf(
                mapOf(
                    "title" to "첫 만남",
                    "eventDate" to "2019-05-10",
                    "eventDatePrecision" to "DAY",
                    "location" to "서울",
                    "emotionTags" to listOf("설렘", "행복"),
                    "text" to "그날의 기억",
                ),
                mapOf(
                    "title" to "마지막 인사",
                    "eventDate" to "2021-03",
                    "eventDatePrecision" to "MONTH",
                    "emotionTags" to listOf("아쉬움"),
                    "letter" to "그때 하지 못한 말을 여기에 남긴다",
                ),
            ),
        )
        every {
            xroomCommandService.createWithMemories(authMemberId, 1L, "고마웠어", any<NewMemories>())
        } returns CreatedXroom(1L, listOf(1L, 2L))

        // When & Then
        mockMvc.postJson("/api/xrooms/with-memories") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect {
                status { isCreated() }
            }
            .andDocument(
                "xroom/create-xroom-with-memories",
                requestBody(
                    targetInfoId(),
                    finalMessage() means "마지막으로 전하는 메시지 (선택)" isOptional true,
                    newMemories(),
                    memoryTitle("memories[].title"),
                    eventDate("memories[].eventDate"),
                    eventDatePrecision("memories[].eventDatePrecision"),
                    location("memories[].location") isOptional true,
                    emotionTags("memories[].emotionTags"),
                    memoryText("memories[].text") isOptional true,
                    memoryLetter("memories[].letter") isOptional true,
                ),
                responseBody(
                    xroomId(),
                    memoryIds(),
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
