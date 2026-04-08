package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.community.api.request.NewPostRequest
import com.konkuk.ma.domain.community.api.response.NewPostResponse
import com.konkuk.ma.domain.community.application.PostCommandService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/posts")
class PostCommandApi(
    private val postCommandService: PostCommandService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal email: String,
        @Valid @RequestBody request: NewPostRequest,
    ): NewPostResponse {
        val postId = postCommandService.create(request.toNewPost(email))
        return NewPostResponse(postId = postId)
    }
}
