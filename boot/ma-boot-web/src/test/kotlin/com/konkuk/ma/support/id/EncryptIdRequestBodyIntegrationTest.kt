package com.konkuk.ma.support.id

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.support.error.GlobalExceptionHandler
import io.kotest.core.spec.style.FunSpec
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

data class EncryptIdTestRequest(
    @field:EncryptId(ObfuscationType.MEMBER)
    val id: Long,
    val name: String
)

data class EncryptIdTestResponse(
    val decodedId: Long,
    val name: String
)

@RestController
class EncryptIdTestController {

    @PostMapping("/test/encrypt-id-request-body")
    fun testEndpoint(@RequestBody request: EncryptIdTestRequest): ResponseEntity<EncryptIdTestResponse> {
        return ResponseEntity.ok(
            EncryptIdTestResponse(
                decodedId = request.id,
                name = request.name
            )
        )
    }
}

@WebMvcTest(EncryptIdTestController::class)
@BaseApiTest
@Import(GlobalExceptionHandler::class)
class EncryptIdRequestBodyIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    private val idObfuscator: IdObfuscator
) : FunSpec({

    context("@EncryptId @RequestBody 역직렬화") {

        test("인코딩된 ID가 포함된 JSON이 Long으로 역직렬화된다") {
            val originalId = 42L
            val encodedId = idObfuscator.encode(ObfuscationType.MEMBER, originalId)
            val requestBody = mapper.writeValueAsString(
                mapOf(
                    "id" to encodedId,
                    "name" to "테스트"
                )
            )

            mockMvc.postJson("/test/encrypt-id-request-body") {
                content = requestBody
            }
                .andExpect {
                    status { isOk() }
                    jsonPath("$.decodedId").value(originalId)
                    jsonPath("$.name").value("테스트")
                }
        }

        test("잘못된 인코딩 ID가 포함된 JSON이면 400을 반환한다") {
            val requestBody = mapper.writeValueAsString(
                mapOf(
                    "id" to "!@#invalid",
                    "name" to "테스트"
                )
            )

            mockMvc.postJson("/test/encrypt-id-request-body") {
                content = requestBody
            }
                .andExpect {
                    status { isBadRequest() }
                }
        }
    }
})
