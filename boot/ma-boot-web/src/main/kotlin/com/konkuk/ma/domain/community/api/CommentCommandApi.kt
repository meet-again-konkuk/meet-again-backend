package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.community.api.request.NewCommentRequest
import com.konkuk.ma.domain.community.api.response.NewCommentResponse
import com.konkuk.ma.domain.community.application.CommentCommandService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/posts/{postId}/comments")
class CommentCommandApi(
    private val commentCommandService: CommentCommandService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal email: String,
        @PathVariable postId: Long,
        @Valid @RequestBody request: NewCommentRequest,
    ): NewCommentResponse {
        val commentId = commentCommandService.create(request.toNewComment(email, postId))
        return NewCommentResponse(commentId = commentId)
    }
}
