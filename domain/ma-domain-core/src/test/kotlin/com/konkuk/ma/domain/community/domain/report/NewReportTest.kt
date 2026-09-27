package com.konkuk.ma.domain.community.domain.report

import com.konkuk.ma.domain.community.fixture.NewReportFixture
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class NewReportTest : FunSpec({

    context("생성") {

        test("서로 다른 신고자·작성자이면 정상적으로 생성되고 상태 기본값은 RECEIVED다") {
            // Given
            val reporterId = 1L
            val targetAuthorId = 2L

            // When
            val newReport = NewReportFixture.create(
                reporterId = reporterId,
                targetAuthorId = targetAuthorId,
            )

            // Then
            newReport.reporterId shouldBe reporterId
            newReport.targetAuthorId shouldBe targetAuthorId
            newReport.status shouldBe ReportStatus.RECEIVED
        }

        test("게시글 신고는 targetType=POST와 원문 제목·내용 스냅샷을 담는다") {
            // Given
            val targetTitle = "신고된 게시글 제목"
            val targetContent = "신고된 게시글 본문"

            // When
            val newReport = NewReportFixture.create(
                targetType = ReportTargetType.POST,
                targetTitle = targetTitle,
                targetContent = targetContent,
            )

            // Then
            newReport.targetType shouldBe ReportTargetType.POST
            newReport.targetTitle shouldBe targetTitle
            newReport.targetContent shouldBe targetContent
        }

        test("댓글 신고는 targetType=COMMENT이고 제목 스냅샷은 null이다") {
            // Given
            val targetContent = "신고된 댓글 내용"

            // When
            val newReport = NewReportFixture.create(
                targetType = ReportTargetType.COMMENT,
                targetTitle = null,
                targetContent = targetContent,
            )

            // Then
            newReport.targetType shouldBe ReportTargetType.COMMENT
            newReport.targetTitle.shouldBeNull()
            newReport.targetContent shouldBe targetContent
        }

        test("detail을 생략하면 detail은 null이다") {
            // When
            val newReport = NewReportFixture.create(detail = null)

            // Then
            newReport.detail.shouldBeNull()
        }
    }

    context("본인 신고 검증") {

        test("신고자와 대상 작성자가 같으면 InvalidValueException이 발생한다") {
            // Given
            val memberId = 10L

            // When & Then
            shouldThrow<InvalidValueException> {
                NewReportFixture.create(reporterId = memberId, targetAuthorId = memberId)
            }
        }
    }

    context("detail 길이 검증") {

        test("detail이 최대 길이(500자)면 생성에 성공한다") {
            // Given
            val detail = "가".repeat(NewReport.MAX_DETAIL_LENGTH)

            // When
            val newReport = NewReportFixture.create(detail = detail)

            // Then
            newReport.detail shouldBe detail
        }

        test("detail이 최대 길이를 1자 초과(501자)하면 InvalidValueException이 발생한다") {
            // Given
            val detail = "가".repeat(NewReport.MAX_DETAIL_LENGTH + 1)

            // When & Then
            shouldThrow<InvalidValueException> {
                NewReportFixture.create(detail = detail)
            }
        }
    }

    context("toReport") {

        test("신고 시점 스냅샷 필드를 그대로 보존한 Report를 생성한다") {
            // Given
            val reportId = 55L
            val newReport = NewReportFixture.create(
                reporterId = 1L,
                targetType = ReportTargetType.POST,
                targetId = 100L,
                targetAuthorId = 2L,
                targetTitle = "제목 스냅샷",
                targetContent = "본문 스냅샷",
                reason = ReportReason.HARASSMENT,
                detail = "부가 설명",
            )

            // When
            val report = newReport.toReport(reportId)

            // Then
            report.id shouldBe reportId
            report.reporterId shouldBe newReport.reporterId
            report.targetType shouldBe newReport.targetType
            report.targetId shouldBe newReport.targetId
            report.targetAuthorId shouldBe newReport.targetAuthorId
            report.targetTitle shouldBe newReport.targetTitle
            report.targetContent shouldBe newReport.targetContent
            report.reason shouldBe newReport.reason
            report.detail shouldBe newReport.detail
            report.status shouldBe newReport.status
        }
    }
})
