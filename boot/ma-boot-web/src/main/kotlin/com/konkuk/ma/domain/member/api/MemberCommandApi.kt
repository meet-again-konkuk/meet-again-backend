package com.konkuk.ma.domain.member.api

import com.konkuk.ma.domain.member.api.request.SignUpRequest
import com.konkuk.ma.domain.member.api.response.SignUpResponse
import com.konkuk.ma.member.application.MemberCommandService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/members")
class MemberCommandApi(
    private val memberCommandService: MemberCommandService
) {
    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    fun signUp(@Valid @RequestBody request: SignUpRequest): SignUpResponse {
        val memberId = memberCommandService.signUp(request.toCommand())
        
        return SignUpResponse(
            memberId = memberId,
            email = request.email,
            nickname = request.nickname
        )
    }
}
