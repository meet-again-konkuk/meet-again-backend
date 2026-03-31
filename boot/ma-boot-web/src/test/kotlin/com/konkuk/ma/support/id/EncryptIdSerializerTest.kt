package com.konkuk.ma.support.id

import com.fasterxml.jackson.core.JsonGenerator
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class EncryptIdSerializerTest : FunSpec({

    val idObfuscator = mockk<IdObfuscator>()
    val serializer = EncryptIdSerializer(idObfuscator, ObfuscationType.MEMBER)

    context("serialize") {

        test("Long 값을 인코딩된 문자열로 직렬화한다") {
            val id = 42L
            val encoded = "kRnB9P3LxYz1"
            every { idObfuscator.encode(ObfuscationType.MEMBER, id) } returns encoded

            val gen = mockk<JsonGenerator>()
            every { gen.writeString(any<String>()) } returns Unit

            serializer.serialize(id, gen, mockk())

            verify { gen.writeString(encoded) }
        }
    }
})
