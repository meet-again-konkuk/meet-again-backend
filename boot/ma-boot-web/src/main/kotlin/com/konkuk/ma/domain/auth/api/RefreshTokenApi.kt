package com.konkuk.ma.domain.auth.api

import com.konkuk.ma.domain.auth.application.RefreshTokenService
import com.konkuk.ma.domain.auth.api.request.RefreshTokenRequest
import com.konkuk.ma.domain.auth.api.response.LoginResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class RefreshTokenApi(
    private val refreshTokenService: RefreshTokenService
) {
    @PostMapping("/refresh-token")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): LoginResponse {
        val loginInfo = refreshTokenService.refreshToken(request.refreshToken)
        return LoginResponse(loginInfo)
    }
}
