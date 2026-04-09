package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email
import java.time.LocalDateTime

class Post(
    val id: Long = 0L,
    val authorEmail: Email,
    val category: PostCategory,
    val title: String,
    val content: String,
    val likes: Int = 0,
    val createdDate: LocalDateTime = LocalDateTime.now(),
)
