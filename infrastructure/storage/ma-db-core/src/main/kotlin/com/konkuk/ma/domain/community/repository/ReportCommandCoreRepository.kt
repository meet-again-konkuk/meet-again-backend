package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.ReportCommandDao
import com.konkuk.ma.domain.community.domain.port.ReportCommandRepository
import com.konkuk.ma.domain.community.domain.report.NewReport
import org.springframework.stereotype.Repository

@Repository
class ReportCommandCoreRepository(
    private val reportCommandDao: ReportCommandDao,
) : ReportCommandRepository {
    override fun save(newReport: NewReport): Long {
        return reportCommandDao.save(newReport)
    }
}
