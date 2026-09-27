package com.konkuk.ma.domain.community.entity

import com.konkuk.ma.domain.community.domain.report.Report
import com.konkuk.ma.domain.community.domain.report.ReportReason
import com.konkuk.ma.domain.community.domain.report.ReportStatus
import com.konkuk.ma.domain.community.domain.report.ReportTargetType
import com.konkuk.ma.domain.community.entity.table.ReportTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime

class ReportEntity(
    val id: Long,
    val reporterId: Long,
    val targetType: ReportTargetType,
    val targetId: Long,
    val targetAuthorId: Long,
    val targetTitle: String?,
    val targetContent: String,
    val reason: ReportReason,
    val detail: String?,
    val status: ReportStatus,
    val createdDate: LocalDateTime,
) {
    fun toDomain(): Report {
        return Report(
            id = id,
            reporterId = reporterId,
            targetType = targetType,
            targetId = targetId,
            targetAuthorId = targetAuthorId,
            targetTitle = targetTitle,
            targetContent = targetContent,
            reason = reason,
            detail = detail,
            status = status,
            createdDate = createdDate,
        )
    }

    companion object {
        fun from(row: ResultRow): ReportEntity {
            return ReportEntity(
                id = row[ReportTable.id].value,
                reporterId = row[ReportTable.reporterId],
                targetType = ReportTargetType.valueOf(row[ReportTable.targetType]),
                targetId = row[ReportTable.targetId],
                targetAuthorId = row[ReportTable.targetAuthorId],
                targetTitle = row[ReportTable.targetTitle],
                targetContent = row[ReportTable.targetContent],
                reason = ReportReason.valueOf(row[ReportTable.reason]),
                detail = row[ReportTable.detail],
                status = ReportStatus.valueOf(row[ReportTable.status]),
                createdDate = row[ReportTable.createdDate],
            )
        }
    }
}
