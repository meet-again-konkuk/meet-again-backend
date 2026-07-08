package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.domain.block.BlockedMemberIds

class Viewer(
    private val viewerId: Long,
    private val likedIds: LikedIds,
    private val blockedMemberIds: BlockedMemberIds = BlockedMemberIds(emptySet()),
) {
    fun isLikedByMe(targetId: Long): Boolean = likedIds.contains(targetId)

    fun isMine(authorId: Long): Boolean = authorId == viewerId

    fun hasBlocked(authorId: Long): Boolean = blockedMemberIds.contains(authorId)
}
