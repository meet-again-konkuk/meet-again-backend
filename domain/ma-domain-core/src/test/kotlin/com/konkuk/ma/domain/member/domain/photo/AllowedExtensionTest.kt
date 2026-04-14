package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.file.AllowedExtension
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class AllowedExtensionTest : BehaviorSpec({

    Given("허용된 확장자 파일명이 주어졌을 때") {
        listOf(
            "photo.jpeg" to "jpeg",
            "photo.jpg" to "jpg",
            "photo.png" to "png",
            "photo.svg" to "svg",
            "photo.webp" to "webp",
        ).forEach { (fileName, expectedExt) ->
            When("${fileName} 파일명으로 생성하면") {
                val extension = AllowedExtension.from(fileName)

                Then("${expectedExt} 확장자가 추출된다") {
                    extension.normalized shouldBe expectedExt
                }
            }
        }
    }

    Given("대소문자가 혼합된 확장자 파일명이 주어졌을 때") {
        listOf(
            "photo.JpG" to "jpg",
            "image.JPEG" to "jpeg",
            "icon.Svg" to "svg",
            "banner.WeBp" to "webp",
            "avatar.Png" to "png",
        ).forEach { (fileName, expectedExt) ->
            When("${fileName} 파일명이 주어지면") {
                val extension = AllowedExtension.from(fileName)

                Then("소문자로 정규화된 ${expectedExt} 확장자가 생성된다") {
                    extension.normalized shouldBe expectedExt
                }
            }
        }
    }

    Given("점이 여러 개 포함된 파일명이 주어졌을 때") {
        listOf(
            "photo.backup.jpg" to "jpg",
            "my.profile.image.png" to "png",
            "file.v2.final.webp" to "webp",
        ).forEach { (fileName, expectedExt) ->
            When("${fileName} 파일명이 주어지면") {
                val extension = AllowedExtension.from(fileName)

                Then("마지막 확장자인 ${expectedExt}가 추출된다") {
                    extension.normalized shouldBe expectedExt
                }
            }
        }
    }

    Given("허용되지 않는 확장자 파일명이 주어졌을 때") {
        listOf("image.gif", "photo.bmp", "virus.exe", "document.pdf", "image.tiff").forEach { fileName ->
            When("${fileName} 파일명이 주어지면") {
                Then("예외가 발생한다") {
                    shouldThrow<InvalidValueException> {
                        AllowedExtension.from(fileName)
                    }
                }
            }
        }
    }

    Given("확장자가 없는 파일명이 주어졌을 때") {
        listOf("noextension", "", ".").forEach { fileName ->
            When("'${fileName}' 파일명이 주어지면") {
                Then("예외가 발생한다") {
                    shouldThrow<InvalidValueException> {
                        AllowedExtension.from(fileName)
                    }
                }
            }
        }
    }

    Given("동일 확장자로 여러 번 생성할 때") {
        When("같은 파일명으로 두 번 생성하면") {
            val first = AllowedExtension.from("a.jpg")
            val second = AllowedExtension.from("b.jpg")

            Then("같은 캐싱된 인스턴스를 반환한다") {
                (first === second) shouldBe true
            }
        }
    }
})
