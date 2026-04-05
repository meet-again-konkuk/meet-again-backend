package com.konkuk.ma.storage

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ThumbnailatorThumbnailGeneratorTest : FunSpec({

    val thumbnailGenerator = ThumbnailatorThumbnailGenerator()

    context("generate") {

        test("지정한 너비로 이미지를 리사이즈한다") {
            // Given
            val originalWidth = 800
            val originalHeight = 600
            val source = createTestImage(originalWidth, originalHeight)
            val targetWidth = 400

            // When
            val result = thumbnailGenerator.generate(source, targetWidth)

            // Then
            val resizedImage = ImageIO.read(ByteArrayInputStream(result))
            resizedImage.width shouldBe targetWidth
        }

        test("종횡비를 유지하여 리사이즈한다") {
            // Given
            val originalWidth = 800
            val originalHeight = 400
            val source = createTestImage(originalWidth, originalHeight)
            val targetWidth = 400

            // When
            val result = thumbnailGenerator.generate(source, targetWidth)

            // Then
            val resizedImage = ImageIO.read(ByteArrayInputStream(result))
            resizedImage.width shouldBe targetWidth
            resizedImage.height shouldBe 200
        }

        test("원본보다 작은 너비로 리사이즈하면 파일 크기가 줄어든다") {
            // Given
            val source = createTestImage(1600, 1200)
            val targetWidth = 400

            // When
            val result = thumbnailGenerator.generate(source, targetWidth)

            // Then
            source.size shouldBeGreaterThan result.size
        }

        test("정사각형 이미지를 리사이즈하면 정사각형이 유지된다") {
            // Given
            val source = createTestImage(1000, 1000)
            val targetWidth = 200

            // When
            val result = thumbnailGenerator.generate(source, targetWidth)

            // Then
            val resizedImage = ImageIO.read(ByteArrayInputStream(result))
            resizedImage.width shouldBe targetWidth
            resizedImage.height shouldBe targetWidth
        }

        test("결과 바이트 배열이 비어있지 않다") {
            // Given
            val source = createTestImage(500, 500)
            val targetWidth = 100

            // When
            val result = thumbnailGenerator.generate(source, targetWidth)

            // Then
            (result.isNotEmpty()) shouldBe true
        }
    }
})

private fun createTestImage(width: Int, height: Int): ByteArray {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()
    graphics.fillRect(0, 0, width, height)
    graphics.dispose()

    val outputStream = ByteArrayOutputStream()
    ImageIO.write(image, "jpg", outputStream)
    return outputStream.toByteArray()
}
