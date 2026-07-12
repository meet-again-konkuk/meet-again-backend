package com.konkuk.ma.domain.auth.api

import com.konkuk.ma.domain.auth.api.request.FindEmailRequest
import com.konkuk.ma.domain.auth.api.response.FindEmailResponse
import com.konkuk.ma.domain.auth.application.FindEmailService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class FindEmailApi(
    private val findEmailService: FindEmailService
) {
    @PostMapping("/find-email")
    fun findEmail(@Valid @RequestBody request: FindEmailRequest): FindEmailResponse {
        val email = findEmailService.findEmail(request.name, request.phone)
        return FindEmailResponse(email)
    }
}
