package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.ReportQueryDao
import com.konkuk.ma.domain.community.domain.port.ReportQueryRepository
import com.konkuk.ma.domain.community.domain.report.ReportTargetType
import org.springframework.stereotype.Repository

@Repository
class ReportQueryCoreRepository(
    private val reportQueryDao: ReportQueryDao,
) : ReportQueryRepository {
    override fun existsActive(reporterId: Long, targetType: ReportTargetType, targetId: Long): Boolean {
        return reportQueryDao.existsActive(reporterId, targetType.name, targetId)
    }
}
