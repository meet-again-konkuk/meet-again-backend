package com.konkuk.ma.integration

import com.konkuk.ma.domain.community.application.ReportCommandService
import com.konkuk.ma.domain.community.domain.report.ReportReason
import com.konkuk.ma.domain.community.domain.report.ReportStatus
import com.konkuk.ma.domain.community.domain.report.ReportTargetType
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.community.entity.table.ReportTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * 신고 등록(REQ-014) Service 레이어 E2E 통합 테스트.
 *
 * ReportCommandService.reportPost/reportComment 가 실제 H2 DB 를 관통하여
 * 대상 타입/식별자/작성자와 신고 시점 원문 스냅샷(제목·내용)을 정확히 저장하는지,
 * 본인 신고(400)·중복 신고(409)·없는 대상(404) 예외 흐름을 검증한다.
 *
 * Service 본문이 아직 TODO 인 TDD RED 단계이므로 MockMvc 대신 Service 빈을 직접 주입해
 * 반환 도메인 객체와 실제 저장 row 를 단언한다(모킹 사각지대 없음).
 */
@SpringBootTest
@ActiveProfiles("test")
class ReportCommandServiceIntegrationTest(
    private val reportCommandService: ReportCommandService,
) : FunSpec({

    var memberSeq = 0L

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, PostTable, CommentTable, ReportTable)
        }
    }

    afterEach {
        transaction {
            ReportTable.deleteAll()
            CommentTable.deleteAll()
            PostTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(ReportTable, CommentTable, PostTable, MemberTable)
        }
    }

    fun insertMember(nickname: String = "회원"): Long {
        memberSeq += 1
        return transaction {
            MemberTable.insertAndGetId {
                it[MemberTable.email] = "member$memberSeq@example.com"
                it[password] = "encoded"
                it[MemberTable.nickname] = nickname
                it[gender] = "MALE"
                it[phoneNumber] = "01012345678"
                it[name] = "김테스트"
                it[birthDate] = LocalDate.of(1990, 1, 1)
                it[region] = "SEOUL"
            }.value
        }
    }

    fun insertPost(authorId: Long, title: String = "신고 대상 제목", content: String = "신고 대상 본문"): Long {
        return transaction {
            PostTable.insertAndGetId {
                it[PostTable.authorId] = authorId
                it[category] = "CHEER"
                it[PostTable.title] = title
                it[PostTable.content] = content
            }.value
        }
    }

    fun insertComment(postId: Long, authorId: Long, content: String = "신고 대상 댓글 내용"): Long {
        return transaction {
            CommentTable.insertAndGetId {
                it[CommentTable.postId] = postId
                it[CommentTable.authorId] = authorId
                it[CommentTable.content] = content
            }.value
        }
    }

    fun findStoredReport(reporterId: Long, targetType: ReportTargetType, targetId: Long): ResultRow? {
        return transaction {
            ReportTable.selectAll()
                .where {
                    (ReportTable.reporterId eq reporterId) and
                        (ReportTable.targetType eq targetType.name) and
                        (ReportTable.targetId eq targetId) and
                        (ReportTable.deleted eq false)
                }
                .singleOrNull()
        }
    }

    context("reportPost") {

        test("게시글을 신고하면 대상 타입/식별자/작성자와 제목·본문 스냅샷이 저장되고 상태는 RECEIVED다") {
            // Given
            val reporterId = insertMember(nickname = "신고자")
            val authorId = insertMember(nickname = "작성자")
            val title = "혐오 표현이 담긴 제목"
            val content = "혐오 표현이 담긴 본문"
            val postId = insertPost(authorId = authorId, title = title, content = content)
            val reason = ReportReason.HATE

            // When
            val report = reportCommandService.reportPost(postId, reporterId, reason, detail = null)

            // Then - 반환 도메인
            report.id.shouldBeGreaterThan(0L)
            report.status shouldBe ReportStatus.RECEIVED

            // Then - 저장된 스냅샷
            val stored = findStoredReport(reporterId, ReportTargetType.POST, postId)
            stored.shouldNotBeNull()
            stored[ReportTable.targetAuthorId] shouldBe authorId
            stored[ReportTable.targetTitle] shouldBe title
            stored[ReportTable.targetContent] shouldBe content
            stored[ReportTable.reason] shouldBe reason.name
            stored[ReportTable.status] shouldBe ReportStatus.RECEIVED.name
        }

        test("존재하지 않는 게시글을 신고하면 EntityNotFoundException이 발생한다") {
            // Given
            val reporterId = insertMember(nickname = "신고자")
            val notExistingPostId = 999_999L

            // When & Then
            shouldThrow<EntityNotFoundException> {
                reportCommandService.reportPost(notExistingPostId, reporterId, ReportReason.SPAM, detail = null)
            }
        }

        test("본인이 작성한 게시글을 신고하면 InvalidValueException이 발생한다") {
            // Given
            val authorId = insertMember(nickname = "작성자")
            val postId = insertPost(authorId = authorId)

            // When & Then
            shouldThrow<InvalidValueException> {
                reportCommandService.reportPost(postId, authorId, ReportReason.SPAM, detail = null)
            }
        }

        test("같은 신고자가 같은 게시글을 다시 신고하면 DuplicateException이 발생한다") {
            // Given
            val reporterId = insertMember(nickname = "신고자")
            val authorId = insertMember(nickname = "작성자")
            val postId = insertPost(authorId = authorId)
            reportCommandService.reportPost(postId, reporterId, ReportReason.SPAM, detail = null)

            // When & Then
            shouldThrow<DuplicateException> {
                reportCommandService.reportPost(postId, reporterId, ReportReason.SPAM, detail = null)
            }
        }
    }

    context("reportComment") {

        test("댓글을 신고하면 targetType=COMMENT·내용 스냅샷이 저장되고 제목 스냅샷은 null이다") {
            // Given
            val reporterId = insertMember(nickname = "신고자")
            val authorId = insertMember(nickname = "작성자")
            val postId = insertPost(authorId = authorId)
            val content = "신고 대상 댓글 본문"
            val commentId = insertComment(postId = postId, authorId = authorId, content = content)

            // When
            val report = reportCommandService.reportComment(commentId, reporterId, ReportReason.HARASSMENT, detail = "괴롭힘")

            // Then
            report.status shouldBe ReportStatus.RECEIVED

            val stored = findStoredReport(reporterId, ReportTargetType.COMMENT, commentId)
            stored.shouldNotBeNull()
            stored[ReportTable.targetAuthorId] shouldBe authorId
            stored[ReportTable.targetTitle].shouldBeNull()
            stored[ReportTable.targetContent] shouldBe content
        }

        test("존재하지 않는 댓글을 신고하면 EntityNotFoundException이 발생한다") {
            // Given
            val reporterId = insertMember(nickname = "신고자")
            val notExistingCommentId = 999_999L

            // When & Then
            shouldThrow<EntityNotFoundException> {
                reportCommandService.reportComment(notExistingCommentId, reporterId, ReportReason.SPAM, detail = null)
            }
        }

        test("본인이 작성한 댓글을 신고하면 InvalidValueException이 발생한다") {
            // Given
            val authorId = insertMember(nickname = "작성자")
            val postId = insertPost(authorId = authorId)
            val commentId = insertComment(postId = postId, authorId = authorId)

            // When & Then
            shouldThrow<InvalidValueException> {
                reportCommandService.reportComment(commentId, authorId, ReportReason.SPAM, detail = null)
            }
        }

        test("같은 신고자가 같은 댓글을 다시 신고하면 DuplicateException이 발생한다") {
            // Given
            val reporterId = insertMember(nickname = "신고자")
            val authorId = insertMember(nickname = "작성자")
            val postId = insertPost(authorId = authorId)
            val commentId = insertComment(postId = postId, authorId = authorId)
            reportCommandService.reportComment(commentId, reporterId, ReportReason.SPAM, detail = null)

            // When & Then
            shouldThrow<DuplicateException> {
                reportCommandService.reportComment(commentId, reporterId, ReportReason.SPAM, detail = null)
            }
        }
    }
})
