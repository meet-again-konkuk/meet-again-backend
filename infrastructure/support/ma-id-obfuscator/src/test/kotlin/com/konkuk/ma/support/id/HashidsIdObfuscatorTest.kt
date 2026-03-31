package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.exception.InvalidObfuscatedIdException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveMinLength

class HashidsIdObfuscatorTest : BehaviorSpec({

    val obfuscator = HashidsIdObfuscator(
        salt = "test-salt",
        minLength = 8
    )

    Given("Long 타입 ID가 주어졌을 때") {
        val id = 42L

        When("인코딩하면") {
            val encoded = obfuscator.encode(id)

            Then("8자 이상의 문자열이 반환된다") {
                encoded shouldHaveMinLength 8
            }

            Then("디코딩하면 원래 값으로 복원된다") {
                obfuscator.decode(encoded) shouldBe id
            }
        }
    }

    Given("서로 다른 ID가 주어졌을 때") {
        val id1 = 1L
        val id2 = 2L

        When("각각 인코딩하면") {
            val encoded1 = obfuscator.encode(id1)
            val encoded2 = obfuscator.encode(id2)

            Then("서로 다른 문자열이 생성된다") {
                encoded1 shouldNotBe encoded2
            }
        }
    }

    Given("잘못된 인코딩 문자열이 주어졌을 때") {
        val invalidEncoded = "!@#invalid"

        When("디코딩하면") {
            Then("InvalidObfuscatedIdException이 발생한다") {
                shouldThrow<InvalidObfuscatedIdException> {
                    obfuscator.decode(invalidEncoded)
                }
            }
        }
    }

    Given("동일한 salt로 생성한 obfuscator는") {
        val anotherObfuscator = HashidsIdObfuscator(
            salt = "test-salt",
            minLength = 8
        )

        When("같은 ID를 인코딩하면") {
            val encoded1 = obfuscator.encode(100L)
            val encoded2 = anotherObfuscator.encode(100L)

            Then("동일한 결과를 반환한다") {
                encoded1 shouldBe encoded2
            }
        }
    }

    Given("다른 salt로 생성한 obfuscator는") {
        val differentSaltObfuscator = HashidsIdObfuscator(
            salt = "different-salt",
            minLength = 8
        )

        When("같은 ID를 인코딩하면") {
            val encoded1 = obfuscator.encode(100L)
            val encoded2 = differentSaltObfuscator.encode(100L)

            Then("다른 결과를 반환한다") {
                encoded1 shouldNotBe encoded2
            }
        }
    }
})
