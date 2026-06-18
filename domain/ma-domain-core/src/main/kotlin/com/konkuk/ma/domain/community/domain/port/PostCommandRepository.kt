package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.domain.NewPost

interface PostCommandRepository {
    fun save(newPost: NewPost): Long
    fun anonymizeAuthor(authorEmail: Email, withdrawnEmail: Email)
}
