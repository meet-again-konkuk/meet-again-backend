package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.xroom.fixture.XroomFixture
import com.konkuk.ma.exception.AccessDeniedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class XroomTest : FunSpec({

    context("validateOwnership") {

        test("작성자가 아니면 예외를 던진다") {
            shouldThrow<AccessDeniedException> {
                XroomFixture.create(ownerId = 1L).validateOwnership(2L)
            }
        }

        test("작성자면 통과한다") {
            XroomFixture.create(ownerId = 1L).validateOwnership(1L)
        }
    }

    context("isOwnedBy") {

        test("ownerId와 같으면 true를 반환한다") {
            val xroom = XroomFixture.create(ownerId = 1L)

            xroom.isOwnedBy(xroom.ownerId).shouldBeTrue()
        }

        test("ownerId와 다르면 false를 반환한다") {
            val xroom = XroomFixture.create(ownerId = 1L)

            xroom.isOwnedBy(2L).shouldBeFalse()
        }
    }

    context("validateRecipient") {

        test("수신한 targetInfoId 집합에 방의 targetInfoId가 있으면 통과한다") {
            val xroom = XroomFixture.create(targetInfoId = 10L)

            xroom.validateRecipient(setOf(xroom.targetInfoId))
        }

        test("수신한 targetInfoId 집합에 방의 targetInfoId가 없으면 예외를 던진다") {
            val xroom = XroomFixture.create(targetInfoId = 10L)

            shouldThrow<AccessDeniedException> {
                xroom.validateRecipient(setOf(20L))
            }
        }

        test("수신한 targetInfoId 집합이 비어 있으면 예외를 던진다") {
            val xroom = XroomFixture.create(targetInfoId = 10L)

            shouldThrow<AccessDeniedException> {
                xroom.validateRecipient(emptySet())
            }
        }
    }

    context("파생 속성") {

        test("templateValue는 템플릿의 wireValue를 반환한다") {
            val xroom = XroomFixture.create(template = XroomTemplate.CHAT_MEMORY)

            xroom.templateValue shouldBe XroomTemplate.CHAT_MEMORY.wireValue
        }

        test("finalMessageValue는 마지막 메시지의 값을 반환한다") {
            val finalMessage = FinalMessage("고마웠어")
            val xroom = XroomFixture.create(finalMessage = finalMessage)

            xroom.finalMessageValue shouldBe finalMessage.value
        }

        test("마지막 메시지가 없으면 finalMessageValue는 null이다") {
            val xroom = XroomFixture.create(finalMessage = null)

            xroom.finalMessageValue shouldBe null
        }
    }

    context("updateFinalMessage") {

        test("새 메시지로 교체한다") {
            val updated = XroomFixture.create().updateFinalMessage("고마웠어")

            updated.finalMessage?.value shouldBe "고마웠어"
        }

        test("null이면 메시지를 비운다") {
            val updated = XroomFixture.create(finalMessage = FinalMessage("이전 메시지")).updateFinalMessage(null)

            updated.finalMessage shouldBe null
        }
    }
})
