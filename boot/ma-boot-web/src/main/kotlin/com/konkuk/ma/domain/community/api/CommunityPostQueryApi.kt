package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.common.domain.page.CursorRequest
import com.konkuk.ma.domain.community.api.response.PostsResponse
import com.konkuk.ma.domain.community.application.PostQueryService
import com.konkuk.ma.domain.community.domain.PostCategory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/posts")
class CommunityPostQueryApi(
    private val postQueryService: PostQueryService,
) {
    @GetMapping
    fun findPosts(
        @RequestParam(required = false) category: PostCategory?,
        @RequestParam(required = false) cursorId: Long?,
        @RequestParam(defaultValue = "20") size: Int,
    ): PostsResponse {
        val cursorResult = postQueryService.find(category, CursorRequest(cursorId, size))
        return PostsResponse.from(cursorResult)
    }
}
