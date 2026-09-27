package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.domain.port.CommentNotificationSettingRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class CommentNotificationSettingSwitcherTest : FunSpec({

    val postQueryRepository = mockk<PostQueryRepository>()
    val commentNotificationSettingRepository = mockk<CommentNotificationSettingRepository>(relaxUnitFun = true)
    val switcher = CommentNotificationSettingSwitcher(
        postQueryRepository,
        commentNotificationSettingRepository,
    )

    beforeEach {
        clearAllMocks()
    }

    context("switch") {

        test("enabled=false면 opt-out 한다") {
            // Given
            val memberId = 1L
            val postId = 10L

            every { postQueryRepository.exists(postId) } returns true
            every { commentNotificationSettingRepository.isOptedOut(memberId, postId) } returns false

            // When
            switcher.switch(memberId, postId, enabled = false)

            // Then
            verify { commentNotificationSettingRepository.optOut(memberId, postId) }
            verify(exactly = 0) { commentNotificationSettingRepository.optIn(any(), any()) }
        }

        test("이미 opt-out 상태면 다시 저장하지 않는다(멱등)") {
            // Given
            val memberId = 1L
            val postId = 10L

            every { postQueryRepository.exists(postId) } returns true
            every { commentNotificationSettingRepository.isOptedOut(memberId, postId) } returns true

            // When
            switcher.switch(memberId, postId, enabled = false)

            // Then
            verify(exactly = 0) { commentNotificationSettingRepository.optOut(any(), any()) }
        }

        test("enabled=true면 opt-in 한다") {
            // Given
            val memberId = 1L
            val postId = 10L

            every { postQueryRepository.exists(postId) } returns true

            // When
            switcher.switch(memberId, postId, enabled = true)

            // Then
            verify { commentNotificationSettingRepository.optIn(memberId, postId) }
            verify(exactly = 0) { commentNotificationSettingRepository.optOut(any(), any()) }
        }

        test("게시글이 존재하지 않으면 EntityNotFoundException이 발생하고 설정을 변경하지 않는다") {
            // Given
            val memberId = 1L
            val postId = 999L

            every { postQueryRepository.exists(postId) } returns false

            // When & Then
            shouldThrow<EntityNotFoundException> {
                switcher.switch(memberId, postId, enabled = false)
            }
            verify(exactly = 0) { commentNotificationSettingRepository.optOut(any(), any()) }
            verify(exactly = 0) { commentNotificationSettingRepository.optIn(any(), any()) }
        }
    }
})
