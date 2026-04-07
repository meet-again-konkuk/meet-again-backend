package com.konkuk.ma.domain.community.domain

import java.time.LocalDateTime

class Post(
    val id: Long = 0L,
    val authorEmail: String,
    val authorNickname: String,
    val category: PostCategory,
    val title: String,
    val content: String,
    val likes: Int = 0,
    val comments: Int = 0,
    val createdDate: LocalDateTime = LocalDateTime.now(),
)
