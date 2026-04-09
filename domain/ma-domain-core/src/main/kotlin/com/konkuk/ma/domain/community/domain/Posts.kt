package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.Members

class Posts(val data: List<Post>) {

    fun extractAuthorEmails(): Set<Email> {
        return data.map { it.authorEmail }.toSet()
    }

    fun combineWithAuthors(members: Members): List<PostWithAuthor> {
        return data.map { post ->
            PostWithAuthor(
                post = post,
                nickname = members.findNickname(post.authorEmail),
            )
        }
    }
}
