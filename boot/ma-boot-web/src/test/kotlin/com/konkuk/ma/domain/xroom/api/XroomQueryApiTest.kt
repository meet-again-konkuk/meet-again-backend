package com.konkuk.ma.domain.xroom.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.xroom.application.XroomQueryService
import com.konkuk.ma.domain.xroom.domain.FinalMessage
import com.konkuk.ma.domain.xroom.domain.MyXrooms
import com.konkuk.ma.domain.xroom.domain.ReceivedXroom
import com.konkuk.ma.domain.xroom.domain.ReceivedXrooms
import com.konkuk.ma.domain.xroom.domain.XroomDetail
import com.konkuk.ma.domain.xroom.domain.media.MemoryPhotoUrls
import com.konkuk.ma.domain.xroom.domain.memory.Memories
import com.konkuk.ma.domain.xroom.fixture.MemoryFixture
import com.konkuk.ma.domain.xroom.fixture.MyXroomFixture
import com.konkuk.ma.domain.xroom.fixture.XroomFixture
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.emotionTags
import com.konkuk.ma.vocabulary.eventDate
import com.konkuk.ma.vocabulary.eventDatePrecision
import com.konkuk.ma.vocabulary.finalMessage
import com.konkuk.ma.vocabulary.location
import com.konkuk.ma.vocabulary.memoryCount
import com.konkuk.ma.vocabulary.memoryId
import com.konkuk.ma.vocabulary.memoryLetter
import com.konkuk.ma.vocabulary.memoryText
import com.konkuk.ma.vocabulary.memoryTitle
import com.konkuk.ma.vocabulary.photoUrl
import com.konkuk.ma.vocabulary.recipientName
import com.konkuk.ma.vocabulary.senderName
import com.konkuk.ma.vocabulary.targetInfoId
import com.konkuk.ma.vocabulary.template
import com.konkuk.ma.vocabulary.xroomId
import com.konkuk.ma.vocabulary.xroomIdPath
import com.konkuk.ma.vocabulary.xroomTitle
import com.konkuk.ma.vocabulary.xroomUpdatedAt
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(XroomQueryApi::class)
@BaseApiTest
class XroomQueryApiTest(
    private val mockMvc: MockMvc,
    private val idObfuscator: IdObfuscator,
    @MockkBean private val xroomQueryService: XroomQueryService,
) : FunSpec({

    val authMemberId = 1L

    fun myXroom(id: Long, targetInfoId: Long, recipientName: String) = MyXroomFixture.create(
        xroom = XroomFixture.create(id = id, ownerId = authMemberId, targetInfoId = targetInfoId),
        recipientName = recipientName,
    )

    test("내가 만든 방 목록 조회 API 문서화") {
        // Given
        val myXrooms = MyXrooms(
            data = listOf(
                myXroom(id = 1L, targetInfoId = 10L, recipientName = "김만남"),
                myXroom(id = 2L, targetInfoId = 20L, recipientName = "이추억"),
            )
        )
        every { xroomQueryService.findMine(authMemberId) } returns myXrooms

        // When & Then
        mockMvc.getJson("/api/xrooms/me") {}
            .andExpect { status { isOk() } }
            .andDocument(
                "xroom/find-my-rooms",
                responseBody(
                    xroomId("rooms[].id"),
                    xroomTitle("rooms[].title"),
                    recipientName("rooms[].recipientName"),
                    targetInfoId("rooms[].targetInfoId"),
                    memoryCount("rooms[].memoryCount"),
                    xroomUpdatedAt("rooms[].updatedAt"),
                ),
            )
    }

    test("수신한 방 목록 조회 API 문서화") {
        // Given
        val receivedXrooms = ReceivedXrooms(
            data = listOf(
                ReceivedXroom(
                    xroom = XroomFixture.create(id = 1L, ownerId = 10L),
                    senderName = "김보냄",
                    memoryCount = 0,
                ),
                ReceivedXroom(
                    xroom = XroomFixture.create(id = 2L, ownerId = 20L),
                    senderName = "이전달",
                    memoryCount = 0,
                ),
            )
        )
        every { xroomQueryService.findReceived(authMemberId) } returns receivedXrooms

        // When & Then
        mockMvc.getJson("/api/xrooms/received") {}
            .andExpect { status { isOk() } }
            .andDocument(
                "xroom/find-received",
                responseBody(
                    xroomId("rooms[].id"),
                    xroomTitle("rooms[].title"),
                    senderName("rooms[].senderName"),
                    memoryCount("rooms[].memoryCount"),
                ),
            )
    }

    test("X룸 상세 조회 API 문서화") {
        // Given
        val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, 1L)
        val xroomDetail = XroomDetail(
            xroom = XroomFixture.create(id = 1L, finalMessage = FinalMessage("고마웠어")),
            recipientName = "김만남",
            memoriesCollection = Memories(listOf(MemoryFixture.create(id = 1L, xroomId = 1L))),
            photoUrls = MemoryPhotoUrls(emptyMap()),
        )
        every { xroomQueryService.findDetail(1L, authMemberId) } returns xroomDetail

        // When & Then
        mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId)
            .andExpect { status { isOk() } }
            .andDocument(
                "xroom/find-xroom-detail",
                pathVariables(
                    xroomIdPath(),
                ),
                responseBody(
                    xroomId("id"),
                    xroomTitle("title"),
                    recipientName("recipientName"),
                    template("template"),
                    finalMessage("finalMessage") isOptional true,
                    memoryId("memories[].id"),
                    memoryTitle("memories[].title"),
                    eventDate("memories[].eventDate"),
                    eventDatePrecision("memories[].eventDatePrecision"),
                    location("memories[].location") isOptional true,
                    emotionTags("memories[].emotionTags"),
                    memoryText("memories[].text") isOptional true,
                    memoryLetter("memories[].letter") isOptional true,
                    photoUrl("memories[].photoUrl") isOptional true,
                ),
            )
    }
})
