package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.community.api.response.CommentResponse
import com.konkuk.ma.domain.community.application.CommentQueryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/comments")
class CommentQueryApi(
    private val commentQueryService: CommentQueryService,
) {
    @GetMapping("/{commentId}")
    fun findDetail(
        @PathVariable commentId: Long,
    ): CommentResponse {
        val commentWithAuthor = commentQueryService.findDetail(commentId)
        return CommentResponse.from(commentWithAuthor)
    }
}
