package com.konkuk.ma.domain.xroom.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.xroom.application.MemoryCommandService
import com.konkuk.ma.domain.xroom.application.command.AddMemoryCommand
import com.konkuk.ma.domain.xroom.fixture.AddMemoryRequestFixture
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.emotionTags
import com.konkuk.ma.vocabulary.eventDate
import com.konkuk.ma.vocabulary.eventDatePrecision
import com.konkuk.ma.vocabulary.location
import com.konkuk.ma.vocabulary.memoryId
import com.konkuk.ma.vocabulary.memoryLetter
import com.konkuk.ma.vocabulary.memoryText
import com.konkuk.ma.vocabulary.memoryTitle
import com.konkuk.ma.vocabulary.xroomIdPath
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(MemoryCommandApi::class)
@BaseApiTest
class MemoryCommandApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    private val idObfuscator: IdObfuscator,
    @MockkBean private val memoryCommandService: MemoryCommandService,
) : FunSpec({

    val authMemberId = 1L

    test("기억 추가 API 문서화") {
        // Given
        val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, 1L)
        val request = AddMemoryRequestFixture.create(location = "서울")
        every {
            memoryCommandService.addMemory(1L, authMemberId, any<AddMemoryCommand>())
        } returns 1L

        // When & Then
        mockMvc.postJson("/api/xrooms/{xroomId}/memories", encodedXroomId) {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isCreated() } }
            .andDocument(
                "xroom/add-memory",
                pathVariables(
                    xroomIdPath(),
                ),
                requestBody(
                    memoryTitle("title"),
                    eventDate("eventDate"),
                    eventDatePrecision("eventDatePrecision"),
                    location("location") isOptional true,
                    emotionTags("emotionTags"),
                    memoryText("text") isOptional true,
                    memoryLetter("letter") isOptional true,
                ),
                responseBody(
                    memoryId("memoryId"),
                ),
            )
    }
})
