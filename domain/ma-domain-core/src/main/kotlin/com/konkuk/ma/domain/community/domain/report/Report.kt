package com.konkuk.ma.domain.community.domain.report

import java.time.LocalDateTime

class Report(
    val id: Long = 0L,
    val reporterId: Long,
    val targetType: ReportTargetType,
    val targetId: Long,
    val targetAuthorId: Long,
    val targetTitle: String? = null,
    val targetContent: String,
    val reason: ReportReason,
    val detail: String? = null,
    val status: ReportStatus = ReportStatus.RECEIVED,
    val createdDate: LocalDateTime = LocalDateTime.now(),
)
