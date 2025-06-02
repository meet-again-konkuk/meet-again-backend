package com.konkuk.ma.domain.member.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.extension.BOOLEAN
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.requestParam
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.extension.responseType
import com.konkuk.ma.member.application.MemberQueryService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(MemberQueryApi::class)
@BaseApiTest
class MemberQueryApiTest(
    private val mockMvc: MockMvc,

    @MockkBean private val memberQueryService: MemberQueryService
) : FunSpec({

    test("닉네임 중복 확인 API 문서화") {
        // Given
        val nickname = "테스트닉네임"

        // When & Then
        every { memberQueryService.checkDuplicatedNickname(nickname) } returns false

        mockMvc.getJson("/api/members/duplicated-nickname")
        { param("nickname", nickname) }
            .andExpect {
                status { { isOk() } }
                jsonPath("$.nickname").value(nickname)
                jsonPath("$.duplicated").value(false) }
            .andDocument(
                "check-duplicated-nickname",
                requestParam(
                    "nickname" requestParam "회원 닉네임" example "holeman79"
                ),
                responseBody(
                    "nickname" responseType STRING means "회원 닉네임",
                    "duplicated" responseType BOOLEAN means "중복 여부"
                )
            )
    }

    test("유효하지 않은 닉네임 형식으로 요청 시 실패") {
        // Given
        val invalidNickname = "a"

        // When & Then
        mockMvc.getJson("/api/members/duplicated-nickname")
        { param("nickname", invalidNickname) }
            .andExpect { status { isBadRequest() } }
            .andDocument(
                "check-duplicated-nickname-invalid-format",
                requestParam(
                    "nickname" requestParam "유효하지 않은 형식의 닉네임" example "a"
                )
            )
    }
})
