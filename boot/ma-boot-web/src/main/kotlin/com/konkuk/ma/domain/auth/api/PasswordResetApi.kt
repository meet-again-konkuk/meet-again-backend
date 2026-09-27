package com.konkuk.ma.domain.auth.api

import com.konkuk.ma.domain.auth.api.request.PasswordResetRequest
import com.konkuk.ma.domain.auth.application.PasswordResetService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class PasswordResetApi(
    private val passwordResetService: PasswordResetService
) {
    @PostMapping("/find-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun resetPassword(@Valid @RequestBody request: PasswordResetRequest) {
        passwordResetService.resetPassword(request.email, request.name, request.phone, request.newPassword)
    }
}
