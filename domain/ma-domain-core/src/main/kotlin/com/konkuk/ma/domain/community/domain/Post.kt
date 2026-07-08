package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityType
import java.time.LocalDateTime

class Post(
    val id: Long = 0L,
    val authorId: Long,
    val category: PostCategory,
    val title: String,
    val content: String,
    val createdDate: LocalDateTime = LocalDateTime.now(),
) {
    fun validateOwnership(memberId: Long) {
        if (authorId != memberId) {
            throw AccessDeniedException(EntityType.COMMUNITY_POST, authorId.toString(), memberId.toString())
        }
    }
}
