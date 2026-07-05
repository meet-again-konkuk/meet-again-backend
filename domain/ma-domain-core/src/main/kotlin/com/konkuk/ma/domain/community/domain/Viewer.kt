package com.konkuk.ma.domain.community.domain

class Viewer(
    private val viewerId: Long,
    private val likedIds: LikedIds,
) {
    fun isLikedByMe(targetId: Long): Boolean = likedIds.contains(targetId)

    fun isMine(authorId: Long): Boolean = authorId == viewerId
}
