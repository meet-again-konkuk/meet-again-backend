package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.common.domain.page.PageRequest
import com.konkuk.ma.domain.common.domain.page.PageResult
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.Posts
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostQueryService(
    private val postQueryRepository: PostQueryRepository,
) {
    fun find(category: PostCategory, pageRequest: PageRequest): PageResult<Posts> {
        return postQueryRepository.find(category, pageRequest)
    }
}
