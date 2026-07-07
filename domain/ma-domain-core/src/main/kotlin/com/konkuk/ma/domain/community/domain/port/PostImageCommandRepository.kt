package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.image.NewPostImage

interface PostImageCommandRepository {
    fun save(newPostImage: NewPostImage): Long
    fun softDeleteByPost(postId: Long, memberId: Long)
    fun delete(imageId: Long)
}
