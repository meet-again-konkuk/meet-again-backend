package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.CommentNotificationSettingDao
import com.konkuk.ma.domain.community.domain.port.CommentNotificationSettingRepository
import org.springframework.stereotype.Repository

@Repository
class CommentNotificationSettingCoreRepository(
    private val commentNotificationSettingDao: CommentNotificationSettingDao,
) : CommentNotificationSettingRepository {
    override fun isOptedOut(memberId: Long, postId: Long): Boolean {
        return commentNotificationSettingDao.isOptedOut(memberId, postId)
    }

    override fun optOut(memberId: Long, postId: Long) {
        commentNotificationSettingDao.optOut(memberId, postId)
    }

    override fun optIn(memberId: Long, postId: Long) {
        commentNotificationSettingDao.optIn(memberId, postId)
    }

    override fun deleteByMember(memberId: Long) {
        commentNotificationSettingDao.deleteByMember(memberId)
    }
}
