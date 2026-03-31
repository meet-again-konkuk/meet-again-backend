package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.port.IdObfuscator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import com.fasterxml.jackson.core.JsonGenerator

class EncryptIdSerializerTest : FunSpec({

    val idObfuscator = mockk<IdObfuscator>()
    val serializer = EncryptIdSerializer()

    beforeSpec {
        EncryptIdHolder.idObfuscator = idObfuscator
    }

    context("serialize") {

        test("Long 값을 인코딩된 문자열로 직렬화한다") {
            val id = 42L
            val encoded = "kRnB9P3L"
            every { idObfuscator.encode(id) } returns encoded

            val gen = mockk<JsonGenerator>()
            every { gen.writeString(any<String>()) } returns Unit

            serializer.serialize(id, gen, mockk())

            verify { gen.writeString(encoded) }
        }
    }
})
