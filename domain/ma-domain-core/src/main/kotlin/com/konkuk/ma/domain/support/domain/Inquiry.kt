package com.konkuk.ma.domain.support.domain

import com.konkuk.ma.domain.common.domain.Email
import java.time.LocalDateTime

class Inquiry(
    val id: Long = 0L,
    val authorEmail: Email,
    val title: String,
    val content: String,
    val createdDate: LocalDateTime = LocalDateTime.now(),
)
