package com.konkuk.ma.domain.support.api

import com.konkuk.ma.domain.support.api.request.NewInquiryRequest
import com.konkuk.ma.domain.support.api.response.NewInquiryResponse
import com.konkuk.ma.domain.support.application.InquiryCommandService
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/domain/inquiries")
class InquiryCommandApi(
    private val inquiryCommandService: InquiryCommandService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @LoginMember memberInfo: MemberInfo,
        @Valid @RequestBody request: NewInquiryRequest,
    ): NewInquiryResponse {
        val inquiryId = inquiryCommandService.create(request.toNewInquiry(memberInfo.id))
        return NewInquiryResponse(inquiryId = inquiryId)
    }
}
