package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.domain.block.BlockedMemberIds
import com.konkuk.ma.domain.community.domain.port.BlockQueryRepository
import com.konkuk.ma.domain.community.domain.port.CommentNotificationSettingRepository
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.notification.domain.NewNotification
import com.konkuk.ma.domain.notification.domain.NotificationType
import com.konkuk.ma.domain.notification.domain.port.NotificationCommandRepository
import org.springframework.stereotype.Component

@Component
class CommentNotificationRegistrar(
    private val postQueryRepository: PostQueryRepository,
    private val commentQueryRepository: CommentQueryRepository,
    private val blockQueryRepository: BlockQueryRepository,
    private val commentNotificationSettingRepository: CommentNotificationSettingRepository,
    private val notificationCommandRepository: NotificationCommandRepository,
) {
    fun register(newComment: NewComment, commentId: Long) {
        val recipientId = resolveRecipient(newComment)
        val actorId = newComment.authorId

        if (recipientId == actorId) return
        if (commentNotificationSettingRepository.isOptedOut(recipientId, newComment.postId)) return
        if (hasBlocked(recipientId, actorId)) return

        notificationCommandRepository.save(
            NewNotification(
                recipientId = recipientId,
                type = resolveType(newComment),
                actorId = actorId,
                postId = newComment.postId,
                commentId = commentId,
            ),
        )
    }

    private fun resolveRecipient(newComment: NewComment): Long {
        if (newComment.hasParent()) {
            return commentQueryRepository.findOne(newComment.parentCommentId!!).authorId
        }
        return postQueryRepository.findOne(newComment.postId).authorId
    }

    private fun resolveType(newComment: NewComment): NotificationType {
        if (newComment.hasParent()) {
            return NotificationType.REPLY_ON_COMMENT
        }
        return NotificationType.COMMENT_ON_POST
    }

    private fun hasBlocked(recipientId: Long, actorId: Long): Boolean {
        return BlockedMemberIds(blockQueryRepository.findBlockedMemberIds(recipientId)).contains(actorId)
    }
}
