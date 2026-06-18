package com.konkuk.ma.domain.point.api

import com.konkuk.ma.domain.point.api.request.ChargePointRequest
import com.konkuk.ma.domain.point.api.response.ChargePointResponse
import com.konkuk.ma.domain.point.application.PointChargeService
import com.konkuk.ma.support.security.MemberInfo
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/points")
class PointChargeApi(
    private val pointChargeService: PointChargeService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun charge(
        @AuthenticationPrincipal memberInfo: MemberInfo,
        @Valid @RequestBody request: ChargePointRequest,
    ): ChargePointResponse {
        val command = request.toCommand(memberInfo.email)
        val result = pointChargeService.charge(command)
        return ChargePointResponse.from(result)
    }
}
