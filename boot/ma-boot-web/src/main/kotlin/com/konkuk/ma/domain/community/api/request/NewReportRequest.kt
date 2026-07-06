package com.konkuk.ma.domain.community.api.request

import com.konkuk.ma.domain.community.domain.report.NewReport
import com.konkuk.ma.domain.community.domain.report.ReportReason
import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.Size

class NewReportRequest(
    val reason: ReportReason,

    @field:Size(max = NewReport.MAX_DETAIL_LENGTH, message = ValidationMessages.REPORT_DETAIL_SIZE)
    val detail: String? = null,
)
