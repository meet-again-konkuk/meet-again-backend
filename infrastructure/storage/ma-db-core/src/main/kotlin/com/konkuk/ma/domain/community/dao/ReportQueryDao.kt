package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.table.ReportTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.springframework.stereotype.Component

@Component
class ReportQueryDao {
    fun existsActive(reporterId: Long, targetType: String, targetId: Long): Boolean {
        return ReportTable
            .activeRows {
                (ReportTable.reporterId eq reporterId) and
                    (ReportTable.targetType eq targetType) and
                    (ReportTable.targetId eq targetId)
            }
            .limit(1)
            .any()
    }
}
