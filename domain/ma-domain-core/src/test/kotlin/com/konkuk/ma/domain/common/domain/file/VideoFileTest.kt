package com.konkuk.ma.domain.common.domain.file

import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VideoFileTest : FunSpec({

    context("create - 정상 생성") {

        test("mp4 확장자면 정상 생성된다") {
            val content = "fake-video".toByteArray()
            val videoFile = VideoFile.create(
                originalFileName = "movie.mp4",
                sizeInBytes = content.size.toLong(),
                content = content,
            )

            videoFile.originalFileName shouldBe "movie.mp4"
            videoFile.extension.normalized shouldBe "mp4"
        }

        test("mov 확장자면 정상 생성된다") {
            val content = "fake-video".toByteArray()
            val videoFile = VideoFile.create(
                originalFileName = "movie.mov",
                sizeInBytes = content.size.toLong(),
                content = content,
            )

            videoFile.extension.normalized shouldBe "mov"
        }

        test("webm 확장자면 정상 생성된다") {
            val content = "fake-video".toByteArray()
            val videoFile = VideoFile.create(
                originalFileName = "movie.webm",
                sizeInBytes = content.size.toLong(),
                content = content,
            )

            videoFile.extension.normalized shouldBe "webm"
        }

        test("대문자 확장자도 lowercase로 정규화되어 정상 생성된다") {
            val content = "fake-video".toByteArray()
            val videoFile = VideoFile.create(
                originalFileName = "movie.MP4",
                sizeInBytes = content.size.toLong(),
                content = content,
            )

            videoFile.extension.normalized shouldBe "mp4"
        }
    }

    context("create - 확장자 검증") {

        test("허용되지 않는 확장자(avi)면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                VideoFile.create(
                    originalFileName = "movie.avi",
                    sizeInBytes = 1024L,
                    content = "fake".toByteArray(),
                )
            }
        }

        test("허용되지 않는 확장자(mkv)면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                VideoFile.create(
                    originalFileName = "movie.mkv",
                    sizeInBytes = 1024L,
                    content = "fake".toByteArray(),
                )
            }
        }

        test("이미지 확장자(jpg)면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                VideoFile.create(
                    originalFileName = "image.jpg",
                    sizeInBytes = 1024L,
                    content = "fake".toByteArray(),
                )
            }
        }
    }

    context("init - 크기 검증") {

        test("100MB를 초과하는 영상 파일이면 InvalidValueException이 발생한다") {
            val overSizedBytes = 100 * 1024 * 1024L + 1

            shouldThrow<InvalidValueException> {
                VideoFile.create(
                    originalFileName = "large.mp4",
                    sizeInBytes = overSizedBytes,
                    content = ByteArray(1) { 0 },
                )
            }
        }

        test("정확히 100MB이면 정상 생성된다") {
            val maxBytes = 100 * 1024 * 1024L

            val videoFile = VideoFile.create(
                originalFileName = "exact.mp4",
                sizeInBytes = maxBytes,
                content = ByteArray(1) { 0 },
            )

            videoFile.sizeInBytes shouldBe maxBytes
        }
    }

    context("init - 빈 컨텐츠 검증") {

        test("content가 비어있으면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                VideoFile.create(
                    originalFileName = "empty.mp4",
                    sizeInBytes = 0,
                    content = byteArrayOf(),
                )
            }
        }
    }
})
