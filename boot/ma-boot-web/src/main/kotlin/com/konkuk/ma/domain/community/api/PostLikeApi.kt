package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.community.api.response.PostLikeResponse
import com.konkuk.ma.domain.community.application.PostLikeService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/posts/{postId}/likes")
class PostLikeApi(
    private val postLikeService: PostLikeService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun like(
        @AuthenticationPrincipal email: String,
        @PathVariable postId: Long,
    ): PostLikeResponse {
        val result = postLikeService.like(postId, email)
        return PostLikeResponse.from(result)
    }

    @DeleteMapping
    fun unlike(
        @AuthenticationPrincipal email: String,
        @PathVariable postId: Long,
    ): PostLikeResponse {
        val result = postLikeService.unlike(postId, email)
        return PostLikeResponse.from(result)
    }
}
