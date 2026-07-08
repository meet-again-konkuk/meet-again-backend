package com.konkuk.ma.domain.common.domain.file

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

/**
 * 이미지 매직바이트 검증(결정 D2) 계약 테스트.
 *
 * ImageSignature.matches(header, extension) 는 "확장자가 주장하는 형식"과 "실제 내용물의 서명"이
 * 일치하는지 판정한다. 확장자만 이미지로 위장한 파일을 걸러내는 것이 목적이다.
 * 지원 형식: jpg/jpeg(FF D8 FF), png(89 50 4E 47), webp(RIFF = 52 49 46 46).
 */
class ImageSignatureTest : FunSpec({

    // 각 형식의 정상 서명으로 시작하는 헤더 (뒤에는 임의의 바이트가 이어진다)
    fun headerStartingWith(vararg signature: Int): ByteArray =
        (signature.map { it.toByte() } + List(16) { 0.toByte() }).toByteArray()

    val jpegHeader = headerStartingWith(0xFF, 0xD8, 0xFF)
    val pngHeader = headerStartingWith(0x89, 0x50, 0x4E, 0x47)
    // RIFF....WEBP — 실제 webp 헤더 형태. enum 정의상 앞 4바이트(RIFF)만으로 판정한다.
    val webpHeader = headerStartingWith(0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50)

    context("matches - 확장자와 서명이 일치하는 정상 케이스") {

        test("jpg 확장자와 JPEG 서명이 일치하면 true 를 반환한다") {
            // Given & When & Then
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
            // Given - 확장자는 jpg 이지만 내용물은 png 서명
            // When & Then
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
            // Given - JPEG 서명은 3바이트인데 헤더는 1바이트뿐
            val tooShort = byteArrayOf(0xFF.toByte())

            // When & Then
            ImageSignature.matches(tooShort, "jpg").shouldBeFalse()
        }

        test("헤더가 비어있으면 false 를 반환한다") {
            ImageSignature.matches(ByteArray(0), "png").shouldBeFalse()
        }
    }
})
