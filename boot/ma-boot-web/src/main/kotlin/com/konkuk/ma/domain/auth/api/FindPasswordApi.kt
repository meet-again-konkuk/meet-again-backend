package com.konkuk.ma.domain.auth.api

import com.konkuk.ma.domain.auth.api.request.FindPasswordRequest
import com.konkuk.ma.domain.auth.application.FindPasswordService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class FindPasswordApi(
    private val findPasswordService: FindPasswordService
) {
    @PostMapping("/find-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun findPassword(@Valid @RequestBody request: FindPasswordRequest) {
        findPasswordService.resetPassword(request.email, request.name, request.phone, request.newPassword)
    }
}
