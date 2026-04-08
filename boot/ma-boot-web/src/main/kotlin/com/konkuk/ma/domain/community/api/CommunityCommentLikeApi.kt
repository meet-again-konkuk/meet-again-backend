package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.community.api.response.CommentLikeResponse
import com.konkuk.ma.domain.community.application.CommentLikeService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/comments/{commentId}/like")
class CommunityCommentLikeApi(
    private val commentLikeService: CommentLikeService,
) {
    @PostMapping
    fun toggle(
        @AuthenticationPrincipal email: String,
        @PathVariable commentId: Long,
    ): CommentLikeResponse {
        val result = commentLikeService.toggle(commentId, email)
        return CommentLikeResponse.from(result)
    }
}
