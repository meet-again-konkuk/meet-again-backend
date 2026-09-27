package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.domain.PostDetails

interface PostCommandRepository {
    fun save(newPost: NewPost): Long
    fun update(postId: Long, details: PostDetails)
    fun softDelete(postId: Long, memberId: Long)
}
