package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PostCommandService(
    private val postCommandRepository: PostCommandRepository,
) {
    fun create(newPost: NewPost): Long {
        return postCommandRepository.save(newPost)
    }
}
