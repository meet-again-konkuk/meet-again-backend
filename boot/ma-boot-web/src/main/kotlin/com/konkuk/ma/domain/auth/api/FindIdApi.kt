package com.konkuk.ma.domain.auth.api

import com.konkuk.ma.domain.auth.api.request.FindIdRequest
import com.konkuk.ma.domain.auth.api.response.FindIdResponse
import com.konkuk.ma.domain.auth.application.FindIdService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class FindIdApi(
    private val findIdService: FindIdService
) {
    @PostMapping("/find-id")
    fun findId(@Valid @RequestBody request: FindIdRequest): FindIdResponse {
        val email = findIdService.findId(request.name, request.phone)
        return FindIdResponse(email)
    }
}
