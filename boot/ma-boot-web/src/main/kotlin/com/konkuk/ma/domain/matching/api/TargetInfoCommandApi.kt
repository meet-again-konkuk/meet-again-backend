package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.api.request.NewTargetInfoRequest
import com.konkuk.ma.domain.matching.api.request.UpdateTargetInfoRequest
import com.konkuk.ma.domain.matching.api.response.NewTargetInfoResponse
import com.konkuk.ma.domain.matching.api.response.TargetInfoResponse
import com.konkuk.ma.domain.matching.application.TargetInfoCommandService
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.support.id.DecryptId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
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

    @PutMapping("/{targetInfoId}")
    fun update(
        @AuthenticationPrincipal email: String,
        @PathVariable @DecryptId(ObfuscationType.TARGET_INFO) targetInfoId: Long,
        @Valid @RequestBody request: UpdateTargetInfoRequest,
    ): TargetInfoResponse {
        val updated = targetInfoCommandService.update(
            id = targetInfoId,
            email = email,
            targetName = request.name,
            middleNumber = request.middleNumber?.let { FourDigit(it) },
            lastNumber = request.lastNumber?.let { FourDigit(it) },
            year = request.year?.let { Year(it) },
            month = request.month?.let { Month(it) },
            day = request.day?.let { Day(it) },
            region = request.region,
        )
        return TargetInfoResponse.from(updated)
    }
}
