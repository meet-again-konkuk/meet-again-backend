package com.konkuk.ma.domain.community.application.command

import com.konkuk.ma.domain.community.domain.Comment
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.report.NewReport
import com.konkuk.ma.domain.community.domain.report.ReportReason
import com.konkuk.ma.domain.community.domain.report.ReportTargetType

class ReportCommand(
    val targetId: Long,
    val reporterId: Long,
    val reason: ReportReason,
    val detail: String?,
) {
    fun toNewReport(post: Post): NewReport {
        return NewReport(
            reporterId = reporterId,
            targetType = ReportTargetType.POST,
            targetId = targetId,
            targetAuthorId = post.authorId,
            targetTitle = post.title,
            targetContent = post.content,
            reason = reason,
            detail = detail,
        )
    }

    fun toNewReport(comment: Comment): NewReport {
        return NewReport(
            reporterId = reporterId,
            targetType = ReportTargetType.COMMENT,
            targetId = targetId,
            targetAuthorId = comment.authorId,
            targetContent = comment.content,
            reason = reason,
            detail = detail,
        )
    }
}
