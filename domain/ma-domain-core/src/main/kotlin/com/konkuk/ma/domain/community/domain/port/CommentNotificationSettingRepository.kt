package com.konkuk.ma.domain.community.domain.port

interface CommentNotificationSettingRepository {
    fun isOptedOut(memberId: Long, postId: Long): Boolean
    fun optOut(memberId: Long, postId: Long)
    fun optIn(memberId: Long, postId: Long)
    fun deleteByMember(memberId: Long)
}
