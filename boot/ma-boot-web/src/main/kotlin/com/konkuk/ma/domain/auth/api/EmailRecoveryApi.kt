package com.konkuk.ma.domain.auth.api

import com.konkuk.ma.domain.auth.api.request.EmailRecoveryRequest
import com.konkuk.ma.domain.auth.api.response.EmailRecoveryResponse
import com.konkuk.ma.domain.auth.application.EmailRecoveryService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class EmailRecoveryApi(
    private val emailRecoveryService: EmailRecoveryService
) {
    @PostMapping("/find-email")
    fun findEmail(@Valid @RequestBody request: EmailRecoveryRequest): EmailRecoveryResponse {
        val email = emailRecoveryService.findEmail(request.name, request.phone)
        return EmailRecoveryResponse(email)
    }
}
