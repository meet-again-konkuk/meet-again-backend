package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class Posts(val data: List<Post>) {

    fun extractAuthorIds(): Set<Long> {
        return data.map { it.authorId }.toSet()
    }

    fun extractIds(): List<Long> {
        return data.map { it.id }
    }

    fun combineWithAuthors(members: Members, likeCounts: LikeCounts): List<PostWithAuthor> {
        return data.map { post ->
            PostWithAuthor(
                post = post,
                nickname = members.findNickname(post.authorId),
                likeCount = likeCounts.countOf(post.id),
            )
        }
    }
}
