package com.konkuk.ma.domain.member.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.category
import com.konkuk.ma.vocabulary.displayName
import io.kotest.core.spec.style.FunSpec
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(MemberEnumApi::class)
@BaseApiTest
class MemberEnumApiTest(
    private val mockMvc: MockMvc
) : FunSpec({

    test("Region enum 전체 조회 API 문서화") {
        mockMvc.getJson("/api/members/regions") { }
            .andExpect {
                status { isOk() }
                jsonPath("$").isArray
                jsonPath("$[0].category").exists()
                jsonPath("$[0].displayName").exists()
            }
            .andDocument(
                "get-region-enums",
                responseBody(
                    category("[].category") means "지역 코드 (enum 이름)",
                    displayName("[].displayName") means "지역 한글명",
                )
            )
    }

    test("Region enum은 17개 값을 반환한다") {
        mockMvc.getJson("/api/members/regions") { }
            .andExpect {
                status { isOk() }
                jsonPath("$.length()").value(17)
            }
    }
})
