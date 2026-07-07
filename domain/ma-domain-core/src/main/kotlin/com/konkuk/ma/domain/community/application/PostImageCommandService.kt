package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.community.domain.image.PostImageProcessor
import com.konkuk.ma.domain.community.domain.image.PostImageUploadResult
import com.konkuk.ma.domain.community.domain.image.PostImageUrlResolver
import com.konkuk.ma.domain.community.domain.port.PostImageCommandRepository
import com.konkuk.ma.domain.community.domain.port.PostImageQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PostImageCommandService(
    private val postImageCommandRepository: PostImageCommandRepository,
    private val postImageQueryRepository: PostImageQueryRepository,
    private val postImageProcessor: PostImageProcessor,
    private val postImageUrlResolver: PostImageUrlResolver,
    private val postQueryRepository: PostQueryRepository,
) {
    fun upload(postId: Long, memberId: Long, photoFile: PhotoFile): PostImageUploadResult {
        val post = postQueryRepository.findOne(postId)
        post.validateOwnership(memberId)

        val existing = postImageQueryRepository.findOneActiveOrNull(postId)
        val newPostImage = postImageProcessor.process(postId, photoFile)
        postImageCommandRepository.softDeleteByPost(postId, memberId)
        val savedId = postImageCommandRepository.save(newPostImage)
        val urls = postImageUrlResolver.resolve(newPostImage.toPostImage(savedId))

        return PostImageUploadResult(
            mediaId = savedId,
            postId = postId,
            imageUrl = urls.imageUrl,
            thumbnailUrl = urls.thumbnailUrl,
            replaced = existing != null,
        )
    }

    fun delete(postId: Long, memberId: Long) {
        val post = postQueryRepository.findOne(postId)
        post.validateOwnership(memberId)
        postImageCommandRepository.softDeleteByPost(postId, memberId)
    }
}
