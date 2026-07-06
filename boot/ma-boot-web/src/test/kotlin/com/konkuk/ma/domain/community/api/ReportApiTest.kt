package com.konkuk.ma.domain.community.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.community.application.ReportCommandService
import com.konkuk.ma.domain.community.domain.report.NewReport
import com.konkuk.ma.domain.community.domain.report.Report
import com.konkuk.ma.domain.community.domain.report.ReportReason
import com.konkuk.ma.domain.community.domain.report.ReportStatus
import com.konkuk.ma.domain.community.domain.report.ReportTargetType
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.commentIdPath
import com.konkuk.ma.vocabulary.postIdPath
import com.konkuk.ma.vocabulary.reportDetail
import com.konkuk.ma.vocabulary.reportId
import com.konkuk.ma.vocabulary.reportReason
import com.konkuk.ma.vocabulary.reportStatus
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(ReportApi::class)
@BaseApiTest
class ReportApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val reportCommandService: ReportCommandService,
) : FunSpec({

    test("게시글 신고 API 문서화") {
        // Given
        val request = mapOf(
            "reason" to "SPAM",
            "detail" to "욕설이 포함되어 있습니다.",
        )

        every {
            reportCommandService.reportPost(1L, 1L, ReportReason.SPAM, "욕설이 포함되어 있습니다.")
        } returns Report(
            id = 1L,
            reporterId = 1L,
            targetType = ReportTargetType.POST,
            targetId = 1L,
            targetAuthorId = 2L,
            targetTitle = "신고 대상 게시글 제목",
            targetContent = "신고 대상 원문 내용",
            reason = ReportReason.SPAM,
            status = ReportStatus.RECEIVED,
        )

        // When & Then
        mockMvc.postJson("/api/community/posts/{postId}/reports", 1L) {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isCreated() } }
            .andDocument(
                "community/report-post",
                pathVariables(
                    postIdPath(),
                ),
                requestBody(
                    reportReason(),
                    reportDetail(),
                ),
                responseBody(
                    reportId(),
                    reportStatus(),
                ),
            )
    }

    test("댓글 신고 API 문서화") {
        // Given
        val request = mapOf(
            "reason" to "HARASSMENT",
            "detail" to "지속적으로 괴롭힙니다.",
        )

        every {
            reportCommandService.reportComment(1L, 1L, ReportReason.HARASSMENT, "지속적으로 괴롭힙니다.")
        } returns Report(
            id = 2L,
            reporterId = 1L,
            targetType = ReportTargetType.COMMENT,
            targetId = 1L,
            targetAuthorId = 2L,
            targetTitle = null,
            targetContent = "신고 대상 댓글 내용",
            reason = ReportReason.HARASSMENT,
            status = ReportStatus.RECEIVED,
        )

        // When & Then
        mockMvc.postJson("/api/community/comments/{commentId}/reports", 1L) {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isCreated() } }
            .andDocument(
                "community/report-comment",
                pathVariables(
                    commentIdPath(),
                ),
                requestBody(
                    reportReason(),
                    reportDetail(),
                ),
                responseBody(
                    reportId(),
                    reportStatus(),
                ),
            )
    }

    test("신고 상세 사유가 최대 글자수를 초과하면 400을 반환한다") {
        // Given
        val request = mapOf(
            "reason" to "SPAM",
            "detail" to "가".repeat(NewReport.MAX_DETAIL_LENGTH + 1),
        )

        // When & Then
        mockMvc.postJson("/api/community/posts/{postId}/reports", 1L) {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isBadRequest() } }
    }
})
