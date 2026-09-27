package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.community.domain.report.Report
import com.konkuk.ma.domain.community.domain.report.ReportStatus

class ReportResponse(
    val reportId: Long,
    val status: ReportStatus,
) {
    companion object {
        fun from(report: Report): ReportResponse {
            return ReportResponse(
                reportId = report.id,
                status = report.status,
            )
        }
    }
}
