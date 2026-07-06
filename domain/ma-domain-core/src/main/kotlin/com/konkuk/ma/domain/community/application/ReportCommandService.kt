package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.community.domain.port.ReportCommandRepository
import com.konkuk.ma.domain.community.domain.report.NewReport
import com.konkuk.ma.domain.community.domain.report.Report
import com.konkuk.ma.domain.community.domain.report.ReportReason
import com.konkuk.ma.domain.community.domain.report.ReportTargetType
import com.konkuk.ma.domain.community.domain.report.ReportValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ReportCommandService(
    private val postQueryRepository: PostQueryRepository,
    private val commentQueryRepository: CommentQueryRepository,
    private val reportCommandRepository: ReportCommandRepository,
    private val reportValidator: ReportValidator,
) {
    fun reportPost(postId: Long, reporterId: Long, reason: ReportReason, detail: String?): Report {
        val post = postQueryRepository.findOne(postId)
        val newReport = NewReport(
            reporterId = reporterId,
            targetType = ReportTargetType.POST,
            targetId = postId,
            targetAuthorId = post.authorId,
            targetTitle = post.title,
            targetContent = post.content,
            reason = reason,
            detail = detail,
        )
        reportValidator.validate(newReport)
        return newReport.toReport(reportCommandRepository.save(newReport))
    }

    fun reportComment(commentId: Long, reporterId: Long, reason: ReportReason, detail: String?): Report {
        val comment = commentQueryRepository.findOne(commentId)
        val newReport = NewReport(
            reporterId = reporterId,
            targetType = ReportTargetType.COMMENT,
            targetId = commentId,
            targetAuthorId = comment.authorId,
            targetTitle = null,
            targetContent = comment.content,
            reason = reason,
            detail = detail,
        )
        reportValidator.validate(newReport)
        return newReport.toReport(reportCommandRepository.save(newReport))
    }
}
