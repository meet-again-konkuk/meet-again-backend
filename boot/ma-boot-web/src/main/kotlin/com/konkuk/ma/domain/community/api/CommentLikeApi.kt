package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.community.api.response.CommentLikeResponse
import com.konkuk.ma.domain.community.application.CommentLikeService
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
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
        @LoginMember memberInfo: MemberInfo,
        @PathVariable commentId: Long,
    ): CommentLikeResponse {
        val result = commentLikeService.like(commentId, memberInfo.id)
        return CommentLikeResponse.from(result)
    }

    @DeleteMapping
    fun unlike(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable commentId: Long,
    ): CommentLikeResponse {
        val result = commentLikeService.unlike(commentId, memberInfo.id)
        return CommentLikeResponse.from(result)
    }
}
