package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec

class XroomBlockItemTest : FunSpec({

    context("validateCompatibility") {

        test("아이템의 compatibleType과 일치하면 예외 없이 통과한다") {
            XroomBlockItem.POLAROID_FRAME.validateCompatibility(XroomBlockType.PHOTO)
        }

        test("아이템의 compatibleType과 일치하지 않으면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockItem.POLAROID_FRAME.validateCompatibility(XroomBlockType.LONG_TEXT)
            }
        }

        test("LONG_TEXT 아이템에 PHOTO 타입을 요청하면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockItem.LETTER_PAPER.validateCompatibility(XroomBlockType.PHOTO)
            }
        }
    }

    context("validateTextLength") {

        test("아이템의 maxTextLength 이하의 텍스트면 예외 없이 통과한다") {
            XroomBlockItem.PLAIN_CARD.validateTextLength("a".repeat(200))
        }

        test("빈 문자열도 예외 없이 통과한다") {
            XroomBlockItem.PLAIN_CARD.validateTextLength("")
        }

        test("아이템의 maxTextLength를 초과하면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockItem.PLAIN_CARD.validateTextLength("a".repeat(201))
            }
        }

        test("LONG_TEXT 아이템도 maxTextLength를 초과하면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockItem.LETTER_PAPER.validateTextLength("a".repeat(2001))
            }
        }

        test("텍스트 길이 제한이 정의되지 않은 아이템이면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockItem.POLAROID_FRAME.validateTextLength("text")
            }
        }
    }

    context("validatePhotoCount") {

        test("정확히 maxPhotoCount와 일치하면 예외 없이 통과한다") {
            XroomBlockItem.POLAROID_FRAME.validatePhotoCount(1)
        }

        test("COLLAGE_3은 사진 3장이 정확히 일치해야 통과한다") {
            XroomBlockItem.COLLAGE_3.validatePhotoCount(3)
        }

        test("maxPhotoCount보다 적으면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockItem.COLLAGE_3.validatePhotoCount(2)
            }
        }

        test("maxPhotoCount보다 많으면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockItem.COLLAGE_3.validatePhotoCount(4)
            }
        }

        test("0장이면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockItem.POLAROID_FRAME.validatePhotoCount(0)
            }
        }

        test("사진 수 제한이 정의되지 않은 아이템이면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockItem.PLAIN_CARD.validatePhotoCount(1)
            }
        }
    }

    context("validateVideoCount") {

        test("정확히 maxVideoCount와 일치하면 예외 없이 통과한다") {
            XroomBlockItem.VIDEO_FRAME.validateVideoCount(1)
        }

        test("maxVideoCount와 다르면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockItem.VIDEO_FRAME.validateVideoCount(2)
            }
        }

        test("0개면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockItem.VIDEO_FRAME.validateVideoCount(0)
            }
        }

        test("영상 수 제한이 정의되지 않은 아이템이면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockItem.POLAROID_FRAME.validateVideoCount(1)
            }
        }
    }
})
