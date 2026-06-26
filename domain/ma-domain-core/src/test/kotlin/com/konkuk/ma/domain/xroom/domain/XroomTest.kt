package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.exception.AccessDeniedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class XroomTest : FunSpec({

    fun xroom(ownerId: Long = 1L, finalMessage: FinalMessage? = null) = Xroom(
        id = 1L,
        ownerId = ownerId,
        targetInfoId = 1L,
        title = XroomTitle.DEFAULT,
        template = XroomTemplate.DEFAULT,
        finalMessage = finalMessage,
        createdDate = LocalDateTime.now(),
    )

    context("validateOwnership") {

        test("작성자가 아니면 예외를 던진다") {
            shouldThrow<AccessDeniedException> {
                xroom(ownerId = 1L).validateOwnership(2L)
            }
        }

        test("작성자면 통과한다") {
            xroom(ownerId = 1L).validateOwnership(1L)
        }
    }

    context("updateFinalMessage") {

        test("새 메시지로 교체한다") {
            val updated = xroom().updateFinalMessage("고마웠어")

            updated.finalMessage?.value shouldBe "고마웠어"
        }

        test("null이면 메시지를 비운다") {
            val updated = xroom(finalMessage = FinalMessage("이전 메시지")).updateFinalMessage(null)

            updated.finalMessage shouldBe null
        }
    }
})
