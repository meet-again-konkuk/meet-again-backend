package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.domain.PostLike
import com.konkuk.ma.domain.community.domain.PostLikeResult
import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PostLikeService(
    private val postLikeRepository: PostLikeRepository,
) {
    fun like(postId: Long, memberEmail: String): PostLikeResult {
        postLikeRepository.save(PostLike(postId = postId, memberEmail = Email(memberEmail)))
        return PostLikeResult.liked(postLikeRepository.count(postId))
    }

    fun unlike(postId: Long, memberEmail: String): PostLikeResult {
        postLikeRepository.delete(postId, Email(memberEmail))
        return PostLikeResult.unliked(postLikeRepository.count(postId))
    }
}
