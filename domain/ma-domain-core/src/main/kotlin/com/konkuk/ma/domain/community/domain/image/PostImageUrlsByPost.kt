package com.konkuk.ma.domain.community.domain.image

class PostImageUrlsByPost(
    val data: Map<Long, PostImageUrls>,
) {
    fun imageUrlOf(postId: Long): String? = data[postId]?.imageUrl

    fun thumbnailUrlOf(postId: Long): String? = data[postId]?.thumbnailUrl

    companion object {
        fun empty(): PostImageUrlsByPost = PostImageUrlsByPost(emptyMap())
    }
}
