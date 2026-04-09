package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.domain.PostLike
import com.konkuk.ma.domain.community.domain.PostLikeResult
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PostLikeService(
    private val postLikeRepository: PostLikeRepository,
    private val postCommandRepository: PostCommandRepository,
) {
    fun like(postId: Long, memberEmail: Email): PostLikeResult {
        postLikeRepository.save(PostLike(postId = postId, memberEmail = memberEmail))
        val likeCount = postCommandRepository.increaseLikes(postId)
        return PostLikeResult.liked(likeCount)
    }

    fun unlike(postId: Long, memberEmail: Email): PostLikeResult {
        postLikeRepository.delete(postId, memberEmail)
        val likeCount = postCommandRepository.decreaseLikes(postId)
        return PostLikeResult.unliked(likeCount)
    }
}
