package com.konkuk.ma.domain.community.domain.image

import com.konkuk.ma.domain.common.domain.file.port.FileUrlResolver
import org.springframework.stereotype.Component

@Component
class PostImageUrlResolver(
    private val fileUrlResolver: FileUrlResolver,
) {
    fun resolve(image: PostImage): PostImageUrls {
        return PostImageUrls(
            imageUrl = fileUrlResolver.resolve(image.storageKey),
            thumbnailUrl = image.thumbnailKey?.let { fileUrlResolver.resolve(it) },
        )
    }

    fun resolveByPosts(images: List<PostImage>): PostImageUrlsByPost {
        return PostImageUrlsByPost(images.associate { it.postId to resolve(it) })
    }
}
