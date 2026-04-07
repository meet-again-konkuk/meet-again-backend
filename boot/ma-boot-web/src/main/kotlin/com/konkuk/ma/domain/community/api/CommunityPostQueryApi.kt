package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.common.domain.page.PageRequest
import com.konkuk.ma.domain.community.api.response.PostsResponse
import com.konkuk.ma.domain.community.application.PostQueryService
import com.konkuk.ma.domain.community.domain.PostCategory
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
        @AuthenticationPrincipal email: String,
        @RequestParam category: PostCategory,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PostsResponse {
        val pageResult = postQueryService.find(category, PageRequest(page, size))
        return PostsResponse.from(pageResult)
    }
}
