package com.konkuk.ma.domain.auth.api

import com.konkuk.ma.domain.auth.api.request.SignUpRequest
import com.konkuk.ma.domain.auth.api.response.SignUpResponse
import com.konkuk.ma.domain.auth.application.SignUpService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class SignUpApi(
    private val signUpService: SignUpService
) {
    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    fun signUp(@Valid @RequestBody request: SignUpRequest): SignUpResponse {
        val memberId = signUpService.signUp(request.toCommand())

        return SignUpResponse(
            memberId = memberId,
            email = request.email,
            nickname = request.nickname
        )
    }
}
