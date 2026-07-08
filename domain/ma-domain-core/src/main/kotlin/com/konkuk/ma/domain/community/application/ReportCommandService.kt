package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.application.command.ReportCommand
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.community.domain.port.ReportCommandRepository
import com.konkuk.ma.domain.community.domain.report.Report
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
    fun reportPost(command: ReportCommand): Report {
        val post = postQueryRepository.findOne(command.targetId)
        val newReport = command.toNewReport(post)
        reportValidator.validate(newReport)
        return newReport.toReport(reportCommandRepository.save(newReport))
    }

    fun reportComment(command: ReportCommand): Report {
        val comment = commentQueryRepository.findOne(command.targetId)
        val newReport = command.toNewReport(comment)
        reportValidator.validate(newReport)
        return newReport.toReport(reportCommandRepository.save(newReport))
    }
}
