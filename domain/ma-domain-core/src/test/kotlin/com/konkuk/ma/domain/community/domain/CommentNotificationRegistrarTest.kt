package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.domain.port.BlockQueryRepository
import com.konkuk.ma.domain.community.domain.port.CommentNotificationSettingRepository
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.community.fixture.CommentFixture
import com.konkuk.ma.domain.community.fixture.NewCommentFixture
import com.konkuk.ma.domain.community.fixture.PostFixture
import com.konkuk.ma.domain.notification.domain.NewNotification
import com.konkuk.ma.domain.notification.domain.NotificationType
import com.konkuk.ma.domain.notification.domain.port.NotificationCommandRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class CommentNotificationRegistrarTest : FunSpec({

    val postQueryRepository = mockk<PostQueryRepository>()
    val commentQueryRepository = mockk<CommentQueryRepository>()
    val blockQueryRepository = mockk<BlockQueryRepository>()
    val commentNotificationSettingRepository = mockk<CommentNotificationSettingRepository>()
    val notificationCommandRepository = mockk<NotificationCommandRepository>()
    val registrar = CommentNotificationRegistrar(
        postQueryRepository,
        commentQueryRepository,
        blockQueryRepository,
        commentNotificationSettingRepository,
        notificationCommandRepository,
    )

    beforeEach {
        clearAllMocks()
    }

    context("register") {

        test("루트 댓글이면 게시글 작성자에게 COMMENT_ON_POST 알림을 저장한다") {
            // Given
            val actorId = 5L
            val postAuthorId = 9L
            val newComment = NewCommentFixture.create(postId = 30L, authorId = actorId, parentCommentId = null)
            val post = PostFixture.create(id = newComment.postId, authorId = postAuthorId)
            val savedNotification = slot<NewNotification>()

            every { postQueryRepository.findOne(newComment.postId) } returns post
            every { commentNotificationSettingRepository.isOptedOut(postAuthorId, newComment.postId) } returns false
            every { blockQueryRepository.findBlockedMemberIds(postAuthorId) } returns emptySet()
            every { notificationCommandRepository.save(capture(savedNotification)) } returns 1L

            // When
            registrar.register(newComment, commentId = 77L)

            // Then
            savedNotification.captured.recipientId shouldBe postAuthorId
            savedNotification.captured.type shouldBe NotificationType.COMMENT_ON_POST
        }

        test("대댓글이면 부모 댓글 작성자에게 REPLY_ON_COMMENT 알림을 저장한다") {
            // Given
            val actorId = 5L
            val parentAuthorId = 8L
            val parentCommentId = 50L
            val newComment = NewCommentFixture.create(postId = 30L, authorId = actorId, parentCommentId = parentCommentId)
            val parentComment = CommentFixture.create(id = parentCommentId, authorId = parentAuthorId)
            val savedNotification = slot<NewNotification>()

            every { commentQueryRepository.findOne(parentCommentId) } returns parentComment
            every { commentNotificationSettingRepository.isOptedOut(parentAuthorId, newComment.postId) } returns false
            every { blockQueryRepository.findBlockedMemberIds(parentAuthorId) } returns emptySet()
            every { notificationCommandRepository.save(capture(savedNotification)) } returns 1L

            // When
            registrar.register(newComment, commentId = 77L)

            // Then
            savedNotification.captured.recipientId shouldBe parentAuthorId
            savedNotification.captured.type shouldBe NotificationType.REPLY_ON_COMMENT
        }

        test("자기 게시글에 자기가 댓글을 달면 알림을 저장하지 않는다") {
            // Given
            val actorId = 5L
            val newComment = NewCommentFixture.create(postId = 30L, authorId = actorId, parentCommentId = null)
            val post = PostFixture.create(id = newComment.postId, authorId = actorId)

            every { postQueryRepository.findOne(newComment.postId) } returns post

            // When
            registrar.register(newComment, commentId = 77L)

            // Then
            verify(exactly = 0) { notificationCommandRepository.save(any()) }
        }

        test("수신자가 알림을 껐으면(opt-out) 알림을 저장하지 않는다") {
            // Given
            val actorId = 5L
            val postAuthorId = 9L
            val newComment = NewCommentFixture.create(postId = 30L, authorId = actorId, parentCommentId = null)
            val post = PostFixture.create(id = newComment.postId, authorId = postAuthorId)

            every { postQueryRepository.findOne(newComment.postId) } returns post
            every { commentNotificationSettingRepository.isOptedOut(postAuthorId, newComment.postId) } returns true

            // When
            registrar.register(newComment, commentId = 77L)

            // Then
            verify(exactly = 0) { notificationCommandRepository.save(any()) }
        }

        test("수신자가 행위자를 차단했으면 알림을 저장하지 않는다") {
            // Given
            val actorId = 5L
            val postAuthorId = 9L
            val newComment = NewCommentFixture.create(postId = 30L, authorId = actorId, parentCommentId = null)
            val post = PostFixture.create(id = newComment.postId, authorId = postAuthorId)

            every { postQueryRepository.findOne(newComment.postId) } returns post
            every { commentNotificationSettingRepository.isOptedOut(postAuthorId, newComment.postId) } returns false
            every { blockQueryRepository.findBlockedMemberIds(postAuthorId) } returns setOf(actorId)

            // When
            registrar.register(newComment, commentId = 77L)

            // Then
            verify(exactly = 0) { notificationCommandRepository.save(any()) }
        }

        test("정상 저장 시 NewNotification의 모든 필드가 올바르게 채워진다") {
            // Given
            val actorId = 5L
            val postAuthorId = 9L
            val commentId = 77L
            val newComment = NewCommentFixture.create(postId = 30L, authorId = actorId, parentCommentId = null)
            val post = PostFixture.create(id = newComment.postId, authorId = postAuthorId)
            val savedNotification = slot<NewNotification>()

            every { postQueryRepository.findOne(newComment.postId) } returns post
            every { commentNotificationSettingRepository.isOptedOut(postAuthorId, newComment.postId) } returns false
            every { blockQueryRepository.findBlockedMemberIds(postAuthorId) } returns emptySet()
            every { notificationCommandRepository.save(capture(savedNotification)) } returns 1L

            // When
            registrar.register(newComment, commentId)

            // Then
            savedNotification.captured.recipientId shouldBe postAuthorId
            savedNotification.captured.type shouldBe NotificationType.COMMENT_ON_POST
            savedNotification.captured.actorId shouldBe newComment.authorId
            savedNotification.captured.postId shouldBe newComment.postId
            savedNotification.captured.commentId shouldBe commentId
        }
    }
})
