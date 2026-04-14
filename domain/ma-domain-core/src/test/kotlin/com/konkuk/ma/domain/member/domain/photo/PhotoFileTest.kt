package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class PhotoFileTest : BehaviorSpec({

    Given("유효한 파일 정보가 주어졌을 때") {
        val content = "fake-image".toByteArray()

        When("PhotoFile을 생성하면") {
            val photoFile = PhotoFile.create(
                originalFileName = "profile.jpg",
                sizeInBytes = content.size.toLong(),
                content = content
            )

            Then("정상 생성된다") {
                photoFile.originalFileName shouldBe "profile.jpg"
                photoFile.extension.normalized shouldBe "jpg"
            }
        }
    }

    Given("10MB를 초과하는 파일이 주어졌을 때") {
        val overSizedBytes = 10 * 1024 * 1024L + 1

        When("PhotoFile을 생성하면") {
            Then("예외가 발생한다") {
                shouldThrow<InvalidValueException> {
                    PhotoFile.create(
                        originalFileName = "large.jpg",
                        sizeInBytes = overSizedBytes,
                        content = ByteArray(1) { 0 }
                    )
                }
            }
        }
    }

    Given("빈 파일 내용이 주어졌을 때") {
        When("PhotoFile을 생성하면") {
            Then("예외가 발생한다") {
                shouldThrow<InvalidValueException> {
                    PhotoFile.create(
                        originalFileName = "empty.jpg",
                        sizeInBytes = 0,
                        content = byteArrayOf()
                    )
                }
            }
        }
    }
})
