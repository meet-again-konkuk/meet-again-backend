package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.TimeAgoCalculator
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
) {
    fun calculateTimeAgo(): String = TimeAgoCalculator.calculate(createdDate)
}
