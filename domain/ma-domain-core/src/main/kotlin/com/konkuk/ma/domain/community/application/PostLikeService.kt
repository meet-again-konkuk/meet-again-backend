package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.PostLikeResult
import com.konkuk.ma.domain.community.domain.PostLiker
import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PostLikeService(
    private val postLiker: PostLiker,
    private val postLikeRepository: PostLikeRepository,
) {
    fun like(postId: Long, memberId: Long): PostLikeResult {
        postLiker.like(postId, memberId)
        return PostLikeResult.liked(postLikeRepository.count(postId))
    }

    fun unlike(postId: Long, memberId: Long): PostLikeResult {
        postLikeRepository.delete(postId, memberId)
        return PostLikeResult.unliked(postLikeRepository.count(postId))
    }
}
