package com.konkuk.ma.domain.support.domain

import java.time.LocalDateTime

class Inquiry(
    val id: Long = 0L,
    val authorId: Long,
    val title: String,
    val content: String,
    val createdDate: LocalDateTime = LocalDateTime.now(),
)
