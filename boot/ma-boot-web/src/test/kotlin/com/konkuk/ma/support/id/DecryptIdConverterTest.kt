package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.exception.InvalidObfuscatedIdException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.convert.TypeDescriptor

class DecryptIdConverterTest : BehaviorSpec({

    val idObfuscator = mockk<IdObfuscator>()
    val converter = DecryptIdConverter(idObfuscator)

    Given("@DecryptId 어노테이션이 있는 타겟 타입일 때") {
        val sourceType = mockk<TypeDescriptor>()
        val targetType = mockk<TypeDescriptor>()
        every { targetType.hasAnnotation(DecryptId::class.java) } returns true

        When("matches를 호출하면") {
            val result = converter.matches(sourceType, targetType)

            Then("true를 반환한다") {
                result shouldBe true
            }
        }
    }

    Given("@DecryptId 어노테이션이 없는 타겟 타입일 때") {
        val sourceType = mockk<TypeDescriptor>()
        val targetType = mockk<TypeDescriptor>()
        every { targetType.hasAnnotation(DecryptId::class.java) } returns false

        When("matches를 호출하면") {
            val result = converter.matches(sourceType, targetType)

            Then("false를 반환한다") {
                result shouldBe false
            }
        }
    }

    Given("유효한 인코딩된 ID가 주어졌을 때") {
        val encoded = "kRnB9P3LxYz1"
        val decoded = 42L
        val annotation = mockk<DecryptId>()
        every { annotation.value } returns ObfuscationType.MEMBER
        every { idObfuscator.decode(ObfuscationType.MEMBER, encoded) } returns decoded

        val sourceType = mockk<TypeDescriptor>()
        val targetType = mockk<TypeDescriptor>()
        every { targetType.getAnnotation(DecryptId::class.java) } returns annotation

        When("convert를 호출하면") {
            val result = converter.convert(encoded, sourceType, targetType)

            Then("디코딩된 Long 값을 반환한다") {
                result shouldBe decoded
            }
        }
    }

    Given("null이 주어졌을 때") {
        val sourceType = mockk<TypeDescriptor>()
        val targetType = mockk<TypeDescriptor>()

        When("convert를 호출하면") {
            val result = converter.convert(null, sourceType, targetType)

            Then("null을 반환한다") {
                result shouldBe null
            }
        }
    }

    Given("잘못된 인코딩된 ID가 주어졌을 때") {
        val invalidEncoded = "!@#invalid"
        val annotation = mockk<DecryptId>()
        every { annotation.value } returns ObfuscationType.MEMBER
        every { idObfuscator.decode(ObfuscationType.MEMBER, invalidEncoded) } throws InvalidObfuscatedIdException(invalidEncoded)

        val sourceType = mockk<TypeDescriptor>()
        val targetType = mockk<TypeDescriptor>()
        every { targetType.getAnnotation(DecryptId::class.java) } returns annotation

        When("convert를 호출하면") {
            Then("InvalidObfuscatedIdException이 발생한다") {
                shouldThrow<InvalidObfuscatedIdException> {
                    converter.convert(invalidEncoded, sourceType, targetType)
                }
            }
        }
    }
})
