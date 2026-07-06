package com.konkuk.ma.domain.community.domain.report

import com.konkuk.ma.domain.community.domain.port.ReportQueryRepository
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Component

@Component
class ReportValidator(
    private val reportQueryRepository: ReportQueryRepository,
) {
    fun validate(newReport: NewReport) {
        if (reportQueryRepository.existsActive(newReport.reporterId, newReport.targetType, newReport.targetId)) {
            throw DuplicateException(EntityType.COMMUNITY_REPORT, "신고", "${newReport.targetType}#${newReport.targetId}")
        }
    }
}
