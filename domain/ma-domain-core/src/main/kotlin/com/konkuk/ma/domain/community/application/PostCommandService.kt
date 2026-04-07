package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PostCommandService(
    private val postCommandRepository: PostCommandRepository,
    private val memberQueryRepository: MemberQueryRepository,
) {
    fun create(email: String, category: PostCategory, title: String, content: String): Long {
        val member = memberQueryRepository.findOne(email)
        val newPost = NewPost(
            authorEmail = email,
            authorNickname = member.nickname,
            category = category,
            title = title,
            content = content,
        )
        return postCommandRepository.save(newPost)
    }
}
