package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.community.api.response.CommentLikeResponse
import com.konkuk.ma.domain.community.application.CommentLikeService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/comments/{commentId}/likes")
class CommentLikeApi(
    private val commentLikeService: CommentLikeService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun like(
        @AuthenticationPrincipal email: String,
        @PathVariable commentId: Long,
    ): CommentLikeResponse {
        val result = commentLikeService.like(commentId, email)
        return CommentLikeResponse.from(result)
    }

    @DeleteMapping
    fun unlike(
        @AuthenticationPrincipal email: String,
        @PathVariable commentId: Long,
    ): CommentLikeResponse {
        val result = commentLikeService.unlike(commentId, email)
        return CommentLikeResponse.from(result)
    }
}
