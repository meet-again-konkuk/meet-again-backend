package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.api.request.NewTargetInfoRequest
import com.konkuk.ma.domain.matching.api.request.UpdateTargetInfoRequest
import com.konkuk.ma.domain.matching.api.response.NewTargetInfoResponse
import com.konkuk.ma.domain.matching.api.response.TargetInfoResponse
import com.konkuk.ma.domain.matching.application.TargetInfoCommandService
import com.konkuk.ma.support.id.DecryptId
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/target-infos")
class TargetInfoCommandApi(
    private val targetInfoCommandService: TargetInfoCommandService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun save(
        @LoginMember memberInfo: MemberInfo,
        @Valid @RequestBody request: NewTargetInfoRequest
    ): NewTargetInfoResponse {
        val newTargetInfo = request.toNewTargetInfo(memberInfo.email)
        val targetInfoId = targetInfoCommandService.register(newTargetInfo)
        return NewTargetInfoResponse(targetInfoId = targetInfoId, registerEmail = memberInfo.email)
    }

    @PutMapping("/{targetInfoId}")
    fun update(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable @DecryptId(ObfuscationType.TARGET_INFO) targetInfoId: Long,
        @Valid @RequestBody request: UpdateTargetInfoRequest
    ): TargetInfoResponse {
        val updated = targetInfoCommandService.update(targetInfoId, memberInfo.email, request.toUpdateTargetInfo())
        return TargetInfoResponse.from(updated)
    }

    @DeleteMapping("/{targetInfoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable @DecryptId(ObfuscationType.TARGET_INFO) targetInfoId: Long,
    ) {
        targetInfoCommandService.delete(targetInfoId, memberInfo.email)
    }
}
