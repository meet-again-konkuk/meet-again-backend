package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.domain.report.NewReport
import com.konkuk.ma.domain.community.entity.table.ReportTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class ReportCommandDao {
    fun save(newReport: NewReport): Long {
        return ReportTable.insertAndGetId {
            it[reporterId] = newReport.reporterId
            it[targetType] = newReport.targetType.name
            it[targetId] = newReport.targetId
            it[targetAuthorId] = newReport.targetAuthorId
            it[targetTitle] = newReport.targetTitle
            it[targetContent] = newReport.targetContent
            it[reason] = newReport.reason.name
            it[detail] = newReport.detail
            it[status] = newReport.status.name
            it[createdBy] = newReport.reporterId.toString()
            it[lastModifiedBy] = newReport.reporterId.toString()
        }.value
    }
}
