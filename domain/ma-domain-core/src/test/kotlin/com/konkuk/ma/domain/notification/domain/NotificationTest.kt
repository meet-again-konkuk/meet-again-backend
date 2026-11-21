package com.konkuk.ma.domain.notification.domain

import com.konkuk.ma.domain.notification.fixture.NotificationFixture
import com.konkuk.ma.exception.AccessDeniedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec

class NotificationTest : FunSpec({

    context("validateRecipient") {

        test("수신자 본인이면 예외 없이 통과한다") {
            val recipientId = 1L
            val notification = NotificationFixture.create(recipientId = recipientId)

            notification.validateRecipient(recipientId)
        }

        test("수신자가 아닌 회원이면 AccessDeniedException이 발생한다") {
            val notification = NotificationFixture.create(recipientId = 1L)
            val otherMemberId = notification.recipientId + 1

            shouldThrow<AccessDeniedException> {
                notification.validateRecipient(otherMemberId)
            }
        }
    }

})
