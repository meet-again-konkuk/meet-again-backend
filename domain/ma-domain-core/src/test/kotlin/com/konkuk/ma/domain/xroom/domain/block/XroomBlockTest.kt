package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.domain.xroom.fixture.XroomBlockDomainFixture
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec

class XroomBlockTest : FunSpec({

    context("validatePhotoCount") {

        test("PHOTO 타입 블록이고 item.maxPhotoCount와 일치하면 예외 없이 통과한다") {
            val block = XroomBlockDomainFixture.photoBlock(item = XroomBlockItem.POLAROID_FRAME)

            block.validatePhotoCount(1)
        }

        test("PHOTO 타입 블록이고 COLLAGE_3 아이템이면 정확히 3장에서 통과한다") {
            val block = XroomBlockDomainFixture.photoBlock(item = XroomBlockItem.COLLAGE_3)

            block.validatePhotoCount(3)
        }

        test("PHOTO 타입이 아닌 블록에 호출하면 InvalidValueException이 발생한다") {
            val block = XroomBlockDomainFixture.shortTextBlock()

            shouldThrow<InvalidValueException> {
                block.validatePhotoCount(1)
            }
        }

        test("item.maxPhotoCount와 다른 개수면 InvalidValueException이 발생한다") {
            val block = XroomBlockDomainFixture.photoBlock(item = XroomBlockItem.POLAROID_FRAME)

            shouldThrow<InvalidValueException> {
                block.validatePhotoCount(2)
            }
        }
    }

    context("validateVideoCount") {

        test("VIDEO 타입 블록이고 item.maxVideoCount와 일치하면 예외 없이 통과한다") {
            val block = XroomBlockDomainFixture.videoBlock(item = XroomBlockItem.VIDEO_FRAME)

            block.validateVideoCount(1)
        }

        test("VIDEO 타입이 아닌 블록에 호출하면 InvalidValueException이 발생한다") {
            val block = XroomBlockDomainFixture.photoBlock()

            shouldThrow<InvalidValueException> {
                block.validateVideoCount(1)
            }
        }

        test("item.maxVideoCount와 다른 개수면 InvalidValueException이 발생한다") {
            val block = XroomBlockDomainFixture.videoBlock(item = XroomBlockItem.VIDEO_FRAME)

            shouldThrow<InvalidValueException> {
                block.validateVideoCount(2)
            }
        }
    }

    context("validateTypeIs") {

        test("실제 타입과 기대 타입이 일치하면 예외 없이 통과한다") {
            val block = XroomBlockDomainFixture.photoBlock()

            block.validateTypeIs(XroomBlockType.PHOTO)
        }

        test("실제 타입과 기대 타입이 다르면 InvalidValueException이 발생한다") {
            val block = XroomBlockDomainFixture.photoBlock()

            shouldThrow<InvalidValueException> {
                block.validateTypeIs(XroomBlockType.VIDEO)
            }
        }
    }
})
