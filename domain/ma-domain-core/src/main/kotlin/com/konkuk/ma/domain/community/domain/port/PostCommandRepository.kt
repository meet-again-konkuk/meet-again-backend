package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.NewPost

interface PostCommandRepository {
    fun save(newPost: NewPost): Long
    fun increaseLikes(postId: Long): Int
    fun decreaseLikes(postId: Long): Int
}
