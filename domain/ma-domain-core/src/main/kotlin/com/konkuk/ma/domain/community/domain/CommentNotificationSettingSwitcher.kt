package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.domain.port.CommentNotificationSettingRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Component

@Component
class CommentNotificationSettingSwitcher(
    private val postQueryRepository: PostQueryRepository,
    private val commentNotificationSettingRepository: CommentNotificationSettingRepository,
) {
    fun switch(memberId: Long, postId: Long, enabled: Boolean) {
        if (!postQueryRepository.exists(postId)) {
            throw EntityNotFoundException(EntityType.COMMUNITY_POST, postId.toString())
        }
        if (enabled) {
            commentNotificationSettingRepository.optIn(memberId, postId)
            return
        }
        if (commentNotificationSettingRepository.isOptedOut(memberId, postId)) {
            return
        }
        commentNotificationSettingRepository.optOut(memberId, postId)
    }
}
