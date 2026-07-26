package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.CommentNotificationSettingSwitcher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CommentNotificationSettingCommandService(
    private val commentNotificationSettingSwitcher: CommentNotificationSettingSwitcher,
) {
    fun set(memberId: Long, postId: Long, enabled: Boolean) {
        commentNotificationSettingSwitcher.switch(memberId, postId, enabled)
    }
}
