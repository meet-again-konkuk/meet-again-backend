package com.konkuk.ma.domain.xroom.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.xroom.application.XroomQueryService
import com.konkuk.ma.domain.xroom.domain.MyXroom
import com.konkuk.ma.domain.xroom.domain.MyXrooms
import com.konkuk.ma.domain.xroom.domain.Xroom
import com.konkuk.ma.domain.xroom.domain.XroomTemplate
import com.konkuk.ma.domain.xroom.domain.XroomTitle
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.memoryCount
import com.konkuk.ma.vocabulary.recipientName
import com.konkuk.ma.vocabulary.targetInfoId
import com.konkuk.ma.vocabulary.xroomId
import com.konkuk.ma.vocabulary.xroomTitle
import com.konkuk.ma.vocabulary.xroomUpdatedAt
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import java.time.LocalDateTime

@WebMvcTest(XroomQueryApi::class)
@BaseApiTest
class XroomQueryApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val xroomQueryService: XroomQueryService,
) : FunSpec({

    val authMemberId = 1L

    fun myXroom(id: Long, targetInfoId: Long, recipientName: String) = MyXroom(
        xroom = Xroom(
            id = id,
            ownerId = authMemberId,
            targetInfoId = targetInfoId,
            title = XroomTitle.DEFAULT,
            template = XroomTemplate.DEFAULT,
            finalMessage = null,
            createdDate = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        ),
        recipientName = recipientName,
        memoryCount = 0,
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
})
