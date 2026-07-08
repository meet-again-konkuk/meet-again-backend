package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.community.domain.report.NewReport
import com.konkuk.ma.domain.community.domain.report.ReportReason
import com.konkuk.ma.domain.community.domain.report.ReportStatus
import com.konkuk.ma.domain.community.domain.report.ReportTargetType

object NewReportFixture {
    fun create(
        reporterId: Long = 1L,
        targetType: ReportTargetType = ReportTargetType.POST,
        targetId: Long = 100L,
        targetAuthorId: Long = 2L,
        targetTitle: String? = "신고 대상 게시글 제목",
        targetContent: String = "신고 대상 원문 내용",
        reason: ReportReason = ReportReason.SPAM,
        detail: String? = null,
        status: ReportStatus = ReportStatus.RECEIVED,
    ): NewReport {
        return NewReport(
            reporterId = reporterId,
            targetType = targetType,
            targetId = targetId,
            targetAuthorId = targetAuthorId,
            targetTitle = targetTitle,
            targetContent = targetContent,
            reason = reason,
            detail = detail,
            status = status,
        )
    }
}
