package com.konkuk.ma.domain.community.domain

class NewPost(
    val authorId: Long,
    details: PostDetails,
) {
    val category: PostCategory = details.category
    val title: String = details.title
    val content: String = details.content
}
