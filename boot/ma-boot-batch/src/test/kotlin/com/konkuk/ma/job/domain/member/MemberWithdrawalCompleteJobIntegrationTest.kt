package com.konkuk.ma.job.domain.member

import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostLikeTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import com.konkuk.ma.domain.matching.entity.table.TargetInfoTable
import com.konkuk.ma.domain.member.entity.table.MemberPhotoTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.domain.point.entity.table.MemberPointTable
import com.konkuk.ma.domain.point.entity.table.PointHistoryTable
import com.konkuk.ma.domain.support.entity.table.InquiryTable
import com.konkuk.ma.domain.xroom.entity.table.XroomTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * 회원 탈퇴 완료 배치(memberWithdrawalCompleteJob) E2E 통합 테스트.
 * 실제 Job 을 H2 DB 위에서 실행하여, 정리 동작을 "관심사(백업 / 회원 익명화 / 하드삭제 / soft삭제 /
 * 작성데이터 익명화 / 미만료 보호)별로 한 테스트씩" 검증한다.
 *
 * 운영 DDL 에는 FK 가 없으나 Exposed 가 생성하는 테스트 스키마에는 FK 가 생기므로,
 * H2 URL 의 INIT(SET REFERENTIAL_INTEGRITY FALSE)로 운영과 동일하게 FK 미적용 상태로 맞춘다.
 */
@SpringBootTest(properties = ["spring.batch.job.enabled=false"])
@ActiveProfiles("test")
class MemberWithdrawalCompleteJobIntegrationTest(
    private val jobLauncher: JobLauncher,
    private val memberWithdrawalCompleteJob: Job,
    @Value("\${backup.local.base-path}") private val backupBasePath: String,
) : FunSpec({

    val allTables = arrayOf(
        MemberTable, RefreshTokenTable, TargetInfoTable, MatchingResultTable,
        MemberPointTable, PointHistoryTable, PostTable, CommentTable,
        PostLikeTable, CommentLikeTable, InquiryTable, XroomTable, MemberPhotoTable,
    )

    // inputDate(2026-06-17) - 유예 7일 → cutoff 2026-06-10. 그 이전이면 만료.
    val inputDate = "2026-06-17"
    val expiredAt = LocalDateTime.of(2026, 6, 1, 0, 0)
    val notExpiredAt = LocalDateTime.of(2026, 6, 16, 0, 0)
    val withdrawingEmail = "withdrawing@example.com"

    fun insertMember(email: String, withdrawalRequestedAt: LocalDateTime?): Long = transaction {
        MemberTable.insertAndGetId {
            it[MemberTable.email] = email
            it[password] = "hashed-password"
            it[nickname] = "닉네임-$email"
            it[gender] = "MALE"
            it[phoneNumber] = "01012345678"
            it[name] = "김원본"
            it[birthDate] = LocalDate.of(1999, 12, 31)
            it[region] = "SEOUL"
            it[MemberTable.withdrawalRequestedAt] = withdrawalRequestedAt
        }.value
    }

    fun insertExpiredMember(email: String = withdrawingEmail): Long = insertMember(email, expiredAt)

    fun insertMatchingResult(registerId: Long, targetId: Long) = transaction {
        MatchingResultTable.insert {
            it[MatchingResultTable.registerId] = registerId
            it[targetInfoId] = 1L
            it[MatchingResultTable.targetId] = targetId
            it[middleNumberMatched] = true
            it[lastNumberMatched] = true
            it[yearMatched] = true
            it[monthMatched] = true
            it[dayMatched] = true
            it[regionMatched] = true
            it[showingExpiryDate] = LocalDateTime.now().plusDays(30)
            it[matchingExpiryDate] = LocalDate.now().plusDays(210)
        }
    }

    fun runCleanupJob(runId: Long): BatchStatus {
        val params = JobParametersBuilder()
            .addString("inputDate", inputDate)
            .addLong("run.id", runId)
            .toJobParameters()
        return jobLauncher.run(memberWithdrawalCompleteJob, params).status
    }

    fun backupFileExists(memberId: Long): Boolean =
        Files.exists(Paths.get(backupBasePath, "withdrawal-backup", "$memberId.json"))

    beforeSpec {
        transaction { SchemaUtils.create(*allTables) }
    }

    afterSpec {
        transaction { SchemaUtils.drop(*allTables) }
    }

    afterEach {
        transaction { allTables.forEach { it.deleteAll() } }
        File(backupBasePath).deleteRecursively()
    }

    test("정리 전에 회원 스냅샷 백업 파일을 남긴다") {
        val memberId = insertExpiredMember()

        runCleanupJob(1L) shouldBe BatchStatus.COMPLETED

        backupFileExists(memberId) shouldBe true
    }

    test("회원을 익명화하고 탈퇴 처리(soft delete)한다") {
        val memberId = insertExpiredMember()

        runCleanupJob(2L) shouldBe BatchStatus.COMPLETED

        transaction {
            val member = MemberTable.selectAll().where { MemberTable.id eq memberId }.single()
            member[MemberTable.email] shouldBe "withdrawn_$memberId@deleted.local"
            member[MemberTable.deleted] shouldBe true
            member[MemberTable.name] shouldBe "탈퇴한회원"
            member[MemberTable.nickname] shouldBe "탈퇴한회원_$memberId"
        }
    }

    test("하드 삭제 대상(좋아요·리프레시토큰)을 물리 삭제한다") {
        val memberId = insertExpiredMember()
        transaction {
            RefreshTokenTable.insert {
                it[RefreshTokenTable.memberId] = memberId
                it[expirationDate] = LocalDateTime.now().plusDays(7)
                it[token] = "refresh-token"
            }
            PostLikeTable.insert { it[postId] = 1L; it[memberEmail] = withdrawingEmail }
            CommentLikeTable.insert { it[commentId] = 1L; it[memberEmail] = withdrawingEmail }
        }

        runCleanupJob(3L) shouldBe BatchStatus.COMPLETED

        transaction {
            RefreshTokenTable.selectAll().where { RefreshTokenTable.memberId eq memberId }.count() shouldBe 0L
            PostLikeTable.selectAll().where { PostLikeTable.memberEmail eq withdrawingEmail }.count() shouldBe 0L
            CommentLikeTable.selectAll().where { CommentLikeTable.memberEmail eq withdrawingEmail }.count() shouldBe 0L
        }
    }

    test("soft delete 대상(타겟정보·포인트·xroom·사진·등록한 매칭)을 논리 삭제한다") {
        val memberId = insertExpiredMember()
        transaction {
            TargetInfoTable.insert { it[registerId] = memberId; it[name] = "홍길동"; it[targetGender] = "FEMALE" }
            MemberPointTable.insert { it[ownerEmail] = withdrawingEmail; it[balance] = 500 }
            XroomTable.insert { it[ownerEmail] = withdrawingEmail; it[targetInfoId] = 1L; it[theme] = "CORK_BOARD" }
            MemberPhotoTable.insert {
                it[memberEmail] = withdrawingEmail
                it[filePath] = "member/profile/$withdrawingEmail/photo.jpg"
                it[originalFileName] = "photo.jpg"
            }
        }
        insertMatchingResult(registerId = memberId, targetId = 999L)

        runCleanupJob(4L) shouldBe BatchStatus.COMPLETED

        transaction {
            TargetInfoTable.selectAll().where { TargetInfoTable.registerId eq memberId }
                .single()[TargetInfoTable.deleted] shouldBe true
            MemberPointTable.selectAll().where { MemberPointTable.ownerEmail eq withdrawingEmail }
                .single()[MemberPointTable.deleted] shouldBe true
            XroomTable.selectAll().where { XroomTable.ownerEmail eq withdrawingEmail }
                .single()[XroomTable.deleted] shouldBe true
            MemberPhotoTable.selectAll().where { MemberPhotoTable.memberEmail eq withdrawingEmail }
                .single()[MemberPhotoTable.deleted] shouldBe true
            MatchingResultTable.selectAll().where { MatchingResultTable.registerId eq memberId }
                .single()[MatchingResultTable.deleted] shouldBe true
        }
    }

    test("작성·소유 데이터(글·댓글·문의·포인트이력)의 이메일을 익명화하고, 내가 타겟인 매칭은 그대로 둔다") {
        val memberId = insertExpiredMember()
        transaction {
            PostTable.insert { it[authorEmail] = withdrawingEmail; it[category] = "SUCCESS_STORY"; it[title] = "글"; it[content] = "내용" }
            CommentTable.insert { it[postId] = 1L; it[authorEmail] = withdrawingEmail; it[content] = "댓글" }
            InquiryTable.insert { it[authorEmail] = withdrawingEmail; it[title] = "문의"; it[content] = "내용" }
            PointHistoryTable.insert {
                it[ownerEmail] = withdrawingEmail
                it[historyType] = "CHARGE"
                it[quantity] = 500
                it[paidAmount] = 5000
                it[idempotencyKey] = "idem-key-1"
            }
        }
        // 다른 회원이 등록했고 내가 타겟인 매칭 → memberId(비PII) 참조라 익명화 불필요, 행도 삭제되지 않음
        insertMatchingResult(registerId = 999L, targetId = memberId)

        runCleanupJob(5L) shouldBe BatchStatus.COMPLETED

        val withdrawnEmail = "withdrawn_$memberId@deleted.local"
        transaction {
            PostTable.selectAll().where { PostTable.authorEmail eq withdrawnEmail }.count() shouldBe 1L
            CommentTable.selectAll().where { CommentTable.authorEmail eq withdrawnEmail }.count() shouldBe 1L
            InquiryTable.selectAll().where { InquiryTable.authorEmail eq withdrawnEmail }.count() shouldBe 1L
            PointHistoryTable.selectAll().where { PointHistoryTable.ownerEmail eq withdrawnEmail }.count() shouldBe 1L
            val targetedMatch = MatchingResultTable.selectAll()
                .where { MatchingResultTable.targetId eq memberId }.single()
            targetedMatch[MatchingResultTable.deleted] shouldBe false
        }
    }

    test("유예가 만료되지 않은 탈퇴 회원은 정리하지 않는다") {
        val email = "recent-withdrawal@example.com"
        val memberId = insertMember(email, notExpiredAt)
        transaction {
            RefreshTokenTable.insert {
                it[RefreshTokenTable.memberId] = memberId
                it[expirationDate] = LocalDateTime.now().plusDays(7)
                it[token] = "refresh-token"
            }
        }

        runCleanupJob(6L) shouldBe BatchStatus.COMPLETED

        transaction {
            val member = MemberTable.selectAll().where { MemberTable.id eq memberId }.single()
            member[MemberTable.email] shouldBe email
            member[MemberTable.deleted] shouldBe false
            RefreshTokenTable.selectAll().where { RefreshTokenTable.memberId eq memberId }.count() shouldBe 1L
        }
        backupFileExists(memberId) shouldBe false
    }
})
