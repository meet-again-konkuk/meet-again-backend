package com.konkuk.ma.domain.notification.application

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.notification.domain.Notification
import com.konkuk.ma.domain.notification.domain.port.NotificationQueryRepository
import com.konkuk.ma.domain.notification.fixture.NotificationFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class NotificationQueryServiceTest : FunSpec({

    val notificationQueryRepository = mockk<NotificationQueryRepository>()
    val memberQueryRepository = mockk<MemberQueryRepository>()
    val service = NotificationQueryService(notificationQueryRepository, memberQueryRepository)

    beforeEach {
        clearAllMocks()
    }

    context("find") {

        test("커서 조회 결과에 actor 닉네임을 조합하고 페이징 플래그를 보존한다") {
            // Given
            val recipientId = 1L
            val cursor = CursorIdCondition.of(cursorId = null)
            val actor = MemberFixture.create(id = 2L, nickname = "홍길동")
            val notification = NotificationFixture.create(recipientId = recipientId, actorId = actor.id)
            val cursorResult = CursorResult(listOf(notification), hasNext = true, nextCursorId = notification.id)

            every { notificationQueryRepository.findByRecipient(recipientId, cursor) } returns cursorResult
            every { memberQueryRepository.findByIds(any()) } returns listOf(actor)

            // When
            val result = service.find(recipientId, cursor)

            // Then
            result.data shouldHaveSize 1
            result.data[0].notification shouldBe notification
            result.data[0].actorNickname shouldBe actor.nickname
            result.hasNext shouldBe cursorResult.hasNext
            result.nextCursorId shouldBe cursorResult.nextCursorId
        }

        test("actor가 탈퇴 회원이라 조회되지 않으면 닉네임을 '알 수 없음'으로 채운다") {
            // Given
            val recipientId = 1L
            val cursor = CursorIdCondition.of(cursorId = null)
            val notification = NotificationFixture.create(recipientId = recipientId, actorId = 99L)
            val cursorResult = CursorResult(listOf(notification), hasNext = false, nextCursorId = null)

            every { notificationQueryRepository.findByRecipient(recipientId, cursor) } returns cursorResult
            every { memberQueryRepository.findByIds(any()) } returns emptyList()

            // When
            val result = service.find(recipientId, cursor)

            // Then
            result.data shouldHaveSize 1
            result.data[0].actorNickname shouldBe "알 수 없음"
        }

        test("조회 결과가 없으면 빈 결과를 반환한다") {
            // Given
            val recipientId = 1L
            val cursor = CursorIdCondition.of(cursorId = null)
            val cursorResult = CursorResult(emptyList<Notification>(), hasNext = false, nextCursorId = null)

            every { notificationQueryRepository.findByRecipient(recipientId, cursor) } returns cursorResult
            every { memberQueryRepository.findByIds(any()) } returns emptyList()

            // When
            val result = service.find(recipientId, cursor)

            // Then
            result.data shouldHaveSize 0
            result.hasNext shouldBe false
            result.nextCursorId shouldBe null
        }
    }

    context("countUnread") {

        test("안읽음 개수를 그대로 반환한다") {
            // Given
            val recipientId = 1L
            val unreadCount = 5L

            every { notificationQueryRepository.countUnread(recipientId) } returns unreadCount

            // When
            val result = service.countUnread(recipientId)

            // Then
            result shouldBe unreadCount
        }
    }
})
