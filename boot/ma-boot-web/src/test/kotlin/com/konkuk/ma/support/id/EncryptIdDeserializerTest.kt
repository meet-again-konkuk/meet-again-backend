package com.konkuk.ma.support.id

import com.fasterxml.jackson.core.JsonParser
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.common.exception.InvalidObfuscatedIdException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class EncryptIdDeserializerTest : FunSpec({

    val idObfuscator = mockk<IdObfuscator>()
    val deserializer = EncryptIdDeserializer(idObfuscator, ObfuscationType.MEMBER)

    context("deserialize") {

        test("인코딩된 문자열을 Long 값으로 역직렬화한다") {
            val encoded = "kRnB9P3LxYz1"
            val decoded = 42L
            every { idObfuscator.decode(ObfuscationType.MEMBER, encoded) } returns decoded

            val parser = mockk<JsonParser>()
            every { parser.valueAsString } returns encoded

            val result = deserializer.deserialize(parser, mockk())

            result shouldBe decoded
        }

        test("잘못된 인코딩 문자열이면 InvalidObfuscatedIdException이 발생한다") {
            val invalidEncoded = "!@#invalid"
            every { idObfuscator.decode(ObfuscationType.MEMBER, invalidEncoded) } throws InvalidObfuscatedIdException(invalidEncoded)

            val parser = mockk<JsonParser>()
            every { parser.valueAsString } returns invalidEncoded

            shouldThrow<InvalidObfuscatedIdException> {
                deserializer.deserialize(parser, mockk())
            }
        }
    }
})
