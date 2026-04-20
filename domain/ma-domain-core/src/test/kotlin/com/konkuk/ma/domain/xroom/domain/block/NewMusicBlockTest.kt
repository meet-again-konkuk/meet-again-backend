package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.domain.xroom.fixture.XroomBlockFixture
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NewMusicBlockTest : FunSpec({

    context("init - 정상 생성") {

        test("musicUrl과 title이 모두 채워져 있으면 정상 생성된다") {
            val block = XroomBlockFixture.newMusicBlock()

            block.type shouldBe XroomBlockType.MUSIC
        }

        test("artist는 null이어도 정상 생성된다") {
            val block = XroomBlockFixture.newMusicBlock(artist = null)

            block.artist shouldBe null
        }
    }

    context("init - 호환성 검증") {

        test("MUSIC 타입과 호환되지 않는 아이템이면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockFixture.newMusicBlock(item = XroomBlockItem.POLAROID_FRAME)
            }
        }
    }

    context("init - 필수 필드 검증") {

        test("musicUrl이 빈 문자열이면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockFixture.newMusicBlock(musicUrl = "")
            }
        }

        test("musicUrl이 공백만 있으면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockFixture.newMusicBlock(musicUrl = "   ")
            }
        }

        test("title이 빈 문자열이면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockFixture.newMusicBlock(title = "")
            }
        }

        test("title이 공백만 있으면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockFixture.newMusicBlock(title = "   ")
            }
        }
    }
})
