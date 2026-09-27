package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.image.PostImage
import java.time.LocalDateTime

interface PostImageQueryRepository {
    fun findOneActiveOrNull(postId: Long): PostImage?
    fun findActiveByPosts(postIds: List<Long>): List<PostImage>
    fun findDeletedBefore(cutoff: LocalDateTime, cursorId: Long?, pageSize: Int): List<PostImage>
}
