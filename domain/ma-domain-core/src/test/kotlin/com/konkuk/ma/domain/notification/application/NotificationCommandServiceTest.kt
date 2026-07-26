package com.konkuk.ma.domain.notification.application

import com.konkuk.ma.domain.notification.domain.port.NotificationCommandRepository
import com.konkuk.ma.domain.notification.domain.port.NotificationQueryRepository
import com.konkuk.ma.domain.notification.fixture.NotificationFixture
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class NotificationCommandServiceTest : FunSpec({

    val notificationQueryRepository = mockk<NotificationQueryRepository>()
    val notificationCommandRepository = mockk<NotificationCommandRepository>(relaxUnitFun = true)
    val service = NotificationCommandService(notificationQueryRepository, notificationCommandRepository)

    beforeEach {
        clearAllMocks()
    }

    context("markAsRead") {

        test("본인 알림이면 읽음 처리한다") {
            // Given
            val notification = NotificationFixture.create(recipientId = 1L)

            every { notificationQueryRepository.findOne(notification.id) } returns notification

            // When
            service.markAsRead(notification.id, notification.recipientId)

            // Then
            verify { notificationCommandRepository.markAsRead(notification.id) }
        }

        test("타인 알림이면 AccessDeniedException이 발생하고 읽음 처리하지 않는다") {
            // Given
            val notification = NotificationFixture.create(recipientId = 1L)
            val otherMemberId = notification.recipientId + 1

            every { notificationQueryRepository.findOne(notification.id) } returns notification

            // When & Then
            shouldThrow<AccessDeniedException> {
                service.markAsRead(notification.id, otherMemberId)
            }
            verify(exactly = 0) { notificationCommandRepository.markAsRead(any()) }
        }

        test("존재하지 않는 알림이면 EntityNotFoundException이 발생한다") {
            // Given
            val nonExistentId = 999L
            val memberId = 1L

            every { notificationQueryRepository.findOne(nonExistentId) } throws
                EntityNotFoundException(EntityType.NOTIFICATION, nonExistentId.toString())

            // When & Then
            shouldThrow<EntityNotFoundException> {
                service.markAsRead(nonExistentId, memberId)
            }
            verify(exactly = 0) { notificationCommandRepository.markAsRead(any()) }
        }
    }

    context("markAllAsRead") {

        test("수신자의 전체 읽음 처리를 위임한다") {
            // Given
            val memberId = 1L

            // When
            service.markAllAsRead(memberId)

            // Then
            verify { notificationCommandRepository.markAllAsRead(memberId) }
        }
    }
})
