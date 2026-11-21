package com.konkuk.ma.domain.common.domain.file

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class ImageSignatureTest : FunSpec({

    fun headerStartingWith(vararg signature: Int): ByteArray =
        (signature.map { it.toByte() } + List(16) { 0.toByte() }).toByteArray()

    val jpegHeader = headerStartingWith(0xFF, 0xD8, 0xFF)
    val pngHeader = headerStartingWith(0x89, 0x50, 0x4E, 0x47)
    val webpHeader = headerStartingWith(0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50)

    context("matches - 확장자와 서명이 일치하는 정상 케이스") {

        test("jpg 확장자와 JPEG 서명이 일치하면 true 를 반환한다") {
            ImageSignature.matches(jpegHeader, "jpg").shouldBeTrue()
        }

        test("jpeg 확장자와 JPEG 서명이 일치하면 true 를 반환한다") {
            ImageSignature.matches(jpegHeader, "jpeg").shouldBeTrue()
        }

        test("png 확장자와 PNG 서명이 일치하면 true 를 반환한다") {
            ImageSignature.matches(pngHeader, "png").shouldBeTrue()
        }

        test("webp 확장자와 RIFF 서명이 일치하면 true 를 반환한다") {
            ImageSignature.matches(webpHeader, "webp").shouldBeTrue()
        }
    }

    context("matches - 확장자만 위장하고 내용물 서명이 다른 케이스") {

        test("jpg 확장자인데 실제 내용은 PNG 서명이면 false 를 반환한다") {
            ImageSignature.matches(pngHeader, "jpg").shouldBeFalse()
        }

        test("png 확장자인데 실제 내용은 JPEG 서명이면 false 를 반환한다") {
            ImageSignature.matches(jpegHeader, "png").shouldBeFalse()
        }

        test("webp 확장자인데 실제 내용은 PNG 서명이면 false 를 반환한다") {
            ImageSignature.matches(pngHeader, "webp").shouldBeFalse()
        }
    }

    context("matches - 지원하지 않는 확장자") {

        test("커뮤니티에서 배제하는 svg 확장자는 서명과 무관하게 false 를 반환한다") {
            ImageSignature.matches(pngHeader, "svg").shouldBeFalse()
        }

        test("지원 목록에 없는 gif 확장자는 false 를 반환한다") {
            ImageSignature.matches(jpegHeader, "gif").shouldBeFalse()
        }
    }

    context("matches - 헤더 길이 엣지 케이스") {

        test("헤더가 서명 길이보다 짧으면 false 를 반환한다") {
            val tooShort = byteArrayOf(0xFF.toByte())

            ImageSignature.matches(tooShort, "jpg").shouldBeFalse()
        }

        test("헤더가 비어있으면 false 를 반환한다") {
            ImageSignature.matches(ByteArray(0), "png").shouldBeFalse()
        }
    }
})
