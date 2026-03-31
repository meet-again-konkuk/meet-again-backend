package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.exception.InvalidObfuscatedIdException
import com.konkuk.ma.domain.common.port.IdObfuscator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import com.fasterxml.jackson.core.JsonParser

class EncryptIdDeserializerTest : FunSpec({

    val idObfuscator = mockk<IdObfuscator>()
    val deserializer = EncryptIdDeserializer()

    beforeSpec {
        EncryptIdHolder.idObfuscator = idObfuscator
    }

    context("deserialize") {

        test("인코딩된 문자열을 Long 값으로 역직렬화한다") {
            val encoded = "kRnB9P3L"
            val decoded = 42L
            every { idObfuscator.decode(encoded) } returns decoded

            val parser = mockk<JsonParser>()
            every { parser.valueAsString } returns encoded

            val result = deserializer.deserialize(parser, mockk())

            result shouldBe decoded
        }

        test("잘못된 인코딩 문자열이면 InvalidObfuscatedIdException이 발생한다") {
            val invalidEncoded = "!@#invalid"
            every { idObfuscator.decode(invalidEncoded) } throws InvalidObfuscatedIdException(invalidEncoded)

            val parser = mockk<JsonParser>()
            every { parser.valueAsString } returns invalidEncoded

            shouldThrow<InvalidObfuscatedIdException> {
                deserializer.deserialize(parser, mockk())
            }
        }
    }
})
