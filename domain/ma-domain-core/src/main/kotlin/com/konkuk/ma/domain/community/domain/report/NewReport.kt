package com.konkuk.ma.domain.community.domain.report

import com.konkuk.ma.exception.InvalidValueException

class NewReport(
    val reporterId: Long,
    val targetType: ReportTargetType,
    val targetId: Long,
    val targetAuthorId: Long,
    val targetTitle: String? = null,
    val targetContent: String,
    val reason: ReportReason,
    val detail: String? = null,
    val status: ReportStatus = ReportStatus.RECEIVED,
) {
    init {
        validateNotSelfReport()
        validateDetailLength()
    }

    private fun validateNotSelfReport() {
        if (reporterId == targetAuthorId) {
            throw InvalidValueException(NewReport::class, reporterId, "본인이 작성한 콘텐츠는 신고할 수 없습니다.")
        }
    }

    private fun validateDetailLength() {
        val detail = detail ?: return
        if (detail.length > MAX_DETAIL_LENGTH) {
            throw InvalidValueException(NewReport::class, detail, "신고 상세 사유는 ${MAX_DETAIL_LENGTH}자 이하여야 합니다.")
        }
    }

    fun toReport(id: Long): Report {
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
        )
    }

    companion object {
        const val MAX_DETAIL_LENGTH = 500
    }
}
