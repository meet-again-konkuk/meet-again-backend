package com.konkuk.ma.domain.auth.api

import com.konkuk.ma.domain.auth.application.LoginService
import com.konkuk.ma.domain.auth.api.request.LoginRequest
import com.konkuk.ma.domain.auth.api.response.LoginResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class LoginApi(
    private val loginService: LoginService
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse {
        val loginInfo = loginService.login(request.toCommand())
        return LoginResponse(loginInfo)
    }
}
