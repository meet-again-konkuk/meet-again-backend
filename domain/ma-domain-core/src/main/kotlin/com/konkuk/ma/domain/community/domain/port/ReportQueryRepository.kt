package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.report.ReportTargetType

interface ReportQueryRepository {
    fun existsActive(reporterId: Long, targetType: ReportTargetType, targetId: Long): Boolean
}
