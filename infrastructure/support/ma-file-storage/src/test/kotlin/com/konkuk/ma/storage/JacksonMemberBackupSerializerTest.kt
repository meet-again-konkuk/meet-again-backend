package com.konkuk.ma.storage

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.community.fixture.CommentFixture
import com.konkuk.ma.domain.community.fixture.CommentLikeFixture
import com.konkuk.ma.domain.community.fixture.PostFixture
import com.konkuk.ma.domain.community.fixture.PostLikeFixture
import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.matching.fixture.TargetInfoFixture
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.fixture.MemberPhotoFixture
import com.konkuk.ma.domain.point.domain.balance.MemberPoint
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.history.PointHistory
import com.konkuk.ma.domain.point.domain.history.PointHistoryType
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import com.konkuk.ma.domain.support.domain.Inquiry
import com.konkuk.ma.domain.withdrawal.domain.MemberBackupView
import com.konkuk.ma.domain.withdrawal.domain.MemberWithdrawalBackup
import com.konkuk.ma.domain.xroom.domain.Xroom
import com.konkuk.ma.domain.xroom.domain.XroomTheme
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.time.LocalDateTime

class JacksonMemberBackupSerializerTest : FunSpec({

    val serializer = JacksonMemberBackupSerializer()

    fun fullBackup(member: Member): MemberWithdrawalBackup {
        val email = member.email.value
        return MemberWithdrawalBackup(
            member = MemberBackupView.from(member),
            targetInfos = listOf(TargetInfoFixture.create(registerId = member.id)),
            registeredMatchingResults = listOf(MatchingResultFixture.create(registerId = member.id)),
            claimedMatchingResults = listOf(MatchingResultFixture.create(targetId = member.id)),
            pointBalance = MemberPoint(id = 1L, ownerEmail = member.email, balance = PointQuantity(500)),
            pointHistories = listOf(
                PointHistory(
                    id = 1L,
                    ownerEmail = member.email,
                    pointProductId = 1L,
                    historyType = PointHistoryType.CHARGE,
                    quantity = PointQuantity(500),
                    paidAmount = Money.wons(5000),
                    paymentMethod = PaymentMethod.CARD,
                    idempotencyKey = "idem-key-1",
                    approvalNumber = "appr-1",
                    createdDate = LocalDateTime.now(),
                ),
            ),
            posts = listOf(PostFixture.create(authorEmail = email)),
            comments = listOf(CommentFixture.create(authorEmail = email)),
            postLikes = listOf(PostLikeFixture.create(memberEmail = email)),
            commentLikes = listOf(CommentLikeFixture.create(memberEmail = email)),
            inquiries = listOf(Inquiry(id = 1L, authorEmail = member.email, title = "문의 제목", content = "문의 내용")),
            xrooms = listOf(
                Xroom(
                    id = 1L,
                    ownerEmail = member.email,
                    targetInfoId = 1L,
                    theme = XroomTheme.CORK_BOARD,
                    createdDate = LocalDateTime.now(),
                ),
            ),
            photo = MemberPhotoFixture.create(memberEmail = email),
        )
    }

    context("serialize") {

        test("회원 전체 스냅샷을 예외 없이 JSON으로 직렬화한다") {
            val backup = fullBackup(MemberFixture.create())

            val bytes = serializer.serialize(backup)

            bytes.isNotEmpty() shouldBe true
        }

        test("전화번호는 마스킹된 값만 담고 원본 중간자리는 노출하지 않는다") {
            val member = MemberFixture.create(phoneNumber = "01099998888")

            val json = String(serializer.serialize(fullBackup(member)))

            json shouldContain "010-****-8888"
            json shouldNotContain "9999"
        }

        test("password 해시는 백업 JSON에 포함되지 않는다") {
            val member = MemberFixture.create(password = "super-secret-hash")

            val json = String(serializer.serialize(fullBackup(member)))

            json shouldNotContain "super-secret-hash"
            json shouldNotContain "password"
        }
    }
})
