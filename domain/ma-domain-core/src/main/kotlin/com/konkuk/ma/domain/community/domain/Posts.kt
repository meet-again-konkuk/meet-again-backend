package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class Posts(val data: List<Post>) {

    fun extractAuthorEmails(): Set<String> {
        return data.map { it.authorEmail }.toSet()
    }

    fun combineWithAuthors(members: Members): List<PostWithAuthor> {
        return data.map { post ->
            PostWithAuthor(
                post = post,
                nickname = members.findNicknameByEmail(post.authorEmail),
            )
        }
    }
}
