package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.community.api.request.NewReportRequest
import com.konkuk.ma.domain.community.api.response.ReportResponse
import com.konkuk.ma.domain.community.application.ReportCommandService
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community")
class ReportApi(
    private val reportCommandService: ReportCommandService,
) {
    @PostMapping("/posts/{postId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    fun reportPost(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable postId: Long,
        @Valid @RequestBody request: NewReportRequest,
    ): ReportResponse {
        val report = reportCommandService.reportPost(request.toReportCommand(postId, memberInfo.id))
        return ReportResponse.from(report)
    }

    @PostMapping("/comments/{commentId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    fun reportComment(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable commentId: Long,
        @Valid @RequestBody request: NewReportRequest,
    ): ReportResponse {
        val report = reportCommandService.reportComment(request.toReportCommand(commentId, memberInfo.id))
        return ReportResponse.from(report)
    }
}
