package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.matching.api.request.NewTargetInfoRequest
import com.konkuk.ma.domain.matching.api.response.NewTargetInfoResponse
import com.konkuk.ma.domain.matching.application.TargetInfoCommandService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/target-infos")
class TargetInfoCommandApi(
    private val targetInfoCommandService: TargetInfoCommandService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun save(
        @AuthenticationPrincipal email: String,
        @Valid @RequestBody request: NewTargetInfoRequest
    ): NewTargetInfoResponse {
        val newTargetInfo = request.toNewTargetInfo(email)
        val targetInfoId = targetInfoCommandService.register(newTargetInfo)
        
        return NewTargetInfoResponse(
            targetInfoId = targetInfoId,
            registerEmail = email
        )
    }
}
