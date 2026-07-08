package com.konkuk.ma.domain.community.domain.block

import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType

class BlockedMemberIds(val data: Set<Long>) {

    fun contains(authorId: Long): Boolean = authorId in data

    fun validateNotBlocked(post: Post) {
        if (contains(post.authorId)) {
            throw EntityNotFoundException(EntityType.COMMUNITY_POST, post.id.toString())
        }
    }
}
