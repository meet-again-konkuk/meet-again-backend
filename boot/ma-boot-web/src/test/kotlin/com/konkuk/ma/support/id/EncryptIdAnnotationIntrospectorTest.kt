package com.konkuk.ma.support.id

import com.fasterxml.jackson.databind.AnnotationIntrospector
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EncryptIdListFieldResponse(
    @EncryptId(ObfuscationType.MEMORY)
    val memoryIds: List<Long>,
)

class EncryptIdSingleFieldResponse(
    @EncryptId(ObfuscationType.XROOM)
    val xroomId: Long,
)

class EncryptIdAnnotationIntrospectorTest : FunSpec({

    val idObfuscator = HashidsIdObfuscator(baseSalt = "test-salt", minLength = 12)
    val mapper = jacksonObjectMapper().apply {
        setAnnotationIntrospector(
            AnnotationIntrospector.pair(
                EncryptIdAnnotationIntrospector(idObfuscator),
                serializationConfig.annotationIntrospector,
            )
        )
    }

    context("List<Long> 필드 직렬화") {

        test("각 원소를 암호화한 문자열 배열로 직렬화하고 순서를 보존한다") {
            val memoryIds = listOf(30L, 10L, 20L)

            val json = mapper.readTree(mapper.writeValueAsString(EncryptIdListFieldResponse(memoryIds)))

            val serialized = json.get("memoryIds")
            serialized.isArray shouldBe true
            serialized.all { it.isTextual } shouldBe true
            serialized.map { it.asText() } shouldBe memoryIds.map { idObfuscator.encode(ObfuscationType.MEMORY, it) }
        }

        test("빈 리스트는 빈 배열로 직렬화된다") {
            val json = mapper.readTree(mapper.writeValueAsString(EncryptIdListFieldResponse(emptyList())))

            val serialized = json.get("memoryIds")
            serialized.isArray shouldBe true
            serialized.size() shouldBe 0
        }
    }

    context("단일 Long 필드 직렬화") {

        test("기존과 동일하게 암호화된 문자열로 직렬화된다") {
            val xroomId = 42L

            val json = mapper.readTree(mapper.writeValueAsString(EncryptIdSingleFieldResponse(xroomId)))

            val serialized = json.get("xroomId")
            serialized.isTextual shouldBe true
            serialized.asText() shouldBe idObfuscator.encode(ObfuscationType.XROOM, xroomId)
        }
    }
})
