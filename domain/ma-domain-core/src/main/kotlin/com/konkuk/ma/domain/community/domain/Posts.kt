package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class Posts(val data: List<Post>) {

    fun extractAuthorIds(): Set<Long> {
        return data.map { it.authorId }.toSet()
    }

    fun extractIds(): List<Long> {
        return data.map { it.id }
    }

    fun combineWithAuthors(
        members: Members,
        likeCounts: LikeCounts,
        commentCounts: CommentCounts,
        viewer: Viewer,
    ): List<PostWithAuthor> {
        return data.map { post ->
            PostWithAuthor(
                post = post,
                nickname = members.findNickname(post.authorId),
                likeCount = likeCounts.countOf(post.id),
                likedByMe = viewer.isLikedByMe(post.id),
                isMine = viewer.isMine(post.authorId),
                commentCount = commentCounts.countOf(post.id),
            )
        }
    }
}
