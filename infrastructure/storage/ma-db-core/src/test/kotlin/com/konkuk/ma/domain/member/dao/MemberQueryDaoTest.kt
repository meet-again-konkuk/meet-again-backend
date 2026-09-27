package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.member.entity.table.MemberTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@ContextConfiguration(classes = [TestDatabaseConfig::class, MemberQueryDao::class])
@DatabaseTest
class MemberQueryDaoTest(
    private val memberQueryDao: MemberQueryDao
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    private fun insertMember(
        email: String = "test@example.com",
        nickname: String = "nick_$email",
        phoneNumber: String = "01012345678",
        deleted: Boolean = false,
        withdrawalRequestedAt: LocalDateTime? = null
    ): Long {
        return MemberTable.insertAndGetId {
            it[MemberTable.email] = email
            it[password] = "password123"
            it[MemberTable.nickname] = nickname
            it[gender] = "MALE"
            it[MemberTable.phoneNumber] = phoneNumber
            it[name] = "김테스트"
            it[birthDate] = LocalDate.of(1990, 1, 1)
            it[region] = "SEOUL"
            it[MemberTable.deleted] = deleted
            it[MemberTable.withdrawalRequestedAt] = withdrawalRequestedAt
        }.value
    }

    init {
        beforeEach {
            SchemaUtils.create(MemberTable)
        }

        afterEach {
            SchemaUtils.drop(MemberTable)
        }

        test("existsByNickname - 닉네임이 존재하는 경우 true를 반환한다") {
            // Given
            val nickname = "testNickname"
            insertMember(nickname = nickname)

            // When
            val result = memberQueryDao.existsByNickname(nickname)

            // Then
            result shouldBe true
        }

        test("existsByNickname - 소프트 삭제된 회원의 닉네임은 존재하지 않는 것으로 본다") {
            // Given — 탈퇴 완료 회원이 남긴 행은 중복 검사 대상이 아니다
            val nickname = "탈퇴한회원_1"
            insertMember(email = "withdrawn@example.com", nickname = nickname, deleted = true)

            // When
            val result = memberQueryDao.existsByNickname(nickname)

            // Then
            result shouldBe false
        }

        test("existsByNickname - 닉네임이 존재하지 않는 경우 false를 반환한다") {
            // Given
            val nickname = "nonExistentNickname"

            // When
            val result = memberQueryDao.existsByNickname(nickname)

            // Then
            result shouldBe false
        }

        test("existsByEmail - 이메일이 존재하는 경우 true를 반환한다") {
            // Given
            val email = "test@example.com"
            insertMember(email = email)

            // When
            val result = memberQueryDao.existsByEmail(email)

            // Then
            result shouldBe true
        }

        test("existsByEmail - 이메일이 존재하지 않는 경우 false를 반환한다") {
            // Given
            val email = "nonexistent@example.com"

            // When
            val result = memberQueryDao.existsByEmail(email)

            // Then
            result shouldBe false
        }

        test("existsByEmail - 대소문자가 다른 이메일로 조회해도 정확히 매치되는 경우만 true를 반환한다") {
            // Given
            val email = "Test@Example.com"
            val searchEmail = "test@example.com"
            insertMember(email = email)

            // When
            val result = memberQueryDao.existsByEmail(searchEmail)

            // Then
            result shouldBe false
        }

        test("existsByNickname - 빈 문자열로 검색하는 경우 false를 반환한다") {
            // Given
            val nickname = ""

            // When
            val result = memberQueryDao.existsByNickname(nickname)

            // Then
            result shouldBe false
        }

        test("existsByEmail - 빈 문자열로 검색하는 경우 false를 반환한다") {
            // Given
            val email = ""

            // When
            val result = memberQueryDao.existsByEmail(email)

            // Then
            result shouldBe false
        }

        test("existsByPhoneNumber - 전화번호가 존재하는 경우 true를 반환한다") {
            // Given
            val phoneNumber = "01099998888"
            insertMember(phoneNumber = phoneNumber)

            // When
            val result = memberQueryDao.existsByPhoneNumber(phoneNumber)

            // Then
            result shouldBe true
        }

        test("existsByPhoneNumber - 전화번호가 존재하지 않는 경우 false를 반환한다") {
            // Given
            val phoneNumber = "01000000000"

            // When
            val result = memberQueryDao.existsByPhoneNumber(phoneNumber)

            // Then
            result shouldBe false
        }

        test("findOne(id) - id로 회원이 존재하는 경우 해당 회원을 반환한다") {
            // Given
            val email = "found@example.com"
            val id = insertMember(email = email)

            // When
            val result = memberQueryDao.findOne(id)

            // Then
            result.shouldNotBeNull()
            result.id shouldBe id
            result.email shouldBe email
        }

        test("findOne(id) - id로 회원이 존재하지 않는 경우 null을 반환한다") {
            // Given
            val nonExistentId = 999L

            // When
            val result = memberQueryDao.findOne(nonExistentId)

            // Then
            result shouldBe null
        }

        test("findOne(id) - 삭제된 회원은 조회되지 않아 null을 반환한다") {
            // Given
            val deletedId = insertMember(deleted = true)

            // When
            val result = memberQueryDao.findOne(deletedId)

            // Then
            result shouldBe null
        }

        test("findByIds - id 목록에 해당하는 회원들을 반환한다") {
            // Given
            val firstId = insertMember(email = "first@example.com", nickname = "first", phoneNumber = "01011112222")
            val secondId = insertMember(email = "second@example.com", nickname = "second", phoneNumber = "01033334444")
            insertMember(email = "other@example.com", nickname = "other", phoneNumber = "01055556666")

            // When
            val result = memberQueryDao.findByIds(setOf(firstId, secondId))

            // Then
            result shouldHaveSize 2
            result.map { it.id } shouldContainExactlyInAnyOrder listOf(firstId, secondId)
        }

        test("findByIds - 삭제된 회원은 결과에서 제외된다") {
            // Given
            val activeId = insertMember(email = "active@example.com", nickname = "active", phoneNumber = "01011112222")
            val deletedId = insertMember(email = "deleted@example.com", nickname = "deleted", phoneNumber = "01033334444", deleted = true)

            // When
            val result = memberQueryDao.findByIds(setOf(activeId, deletedId))

            // Then
            result shouldHaveSize 1
            result.map { it.id } shouldContainExactlyInAnyOrder listOf(activeId)
        }

        test("findByIds - 빈 id 목록이면 빈 리스트를 반환한다") {
            // Given
            insertMember()

            // When
            val result = memberQueryDao.findByIds(emptySet())

            // Then
            result shouldHaveSize 0
        }

        context("findExpiredWithdrawalRequests") {

            val cutoff = LocalDateTime.of(2026, 6, 1, 0, 0)

            test("컷오프 이전에 탈퇴 신청한 회원을 반환한다") {
                // Given
                insertMember(email = "expired@example.com", nickname = "n1", withdrawalRequestedAt = cutoff.minusDays(1))

                // When
                val result = memberQueryDao.findExpiredWithdrawalRequests(cutoff, null, 10)

                // Then
                result shouldHaveSize 1
                result[0].email shouldBe "expired@example.com"
            }

            test("컷오프 이후(유예 중) 신청 회원은 제외한다") {
                // Given
                insertMember(email = "pending@example.com", nickname = "n2", withdrawalRequestedAt = cutoff.plusDays(1))

                // When
                val result = memberQueryDao.findExpiredWithdrawalRequests(cutoff, null, 10)

                // Then
                result shouldHaveSize 0
            }

            test("탈퇴 신청하지 않은 회원은 제외한다") {
                // Given
                insertMember(email = "active@example.com", nickname = "n3", withdrawalRequestedAt = null)

                // When
                val result = memberQueryDao.findExpiredWithdrawalRequests(cutoff, null, 10)

                // Then
                result shouldHaveSize 0
            }

            test("cursorId 이후의 회원만 반환한다") {
                // Given
                insertMember(email = "a@example.com", nickname = "na", withdrawalRequestedAt = cutoff.minusDays(1))
                insertMember(email = "b@example.com", nickname = "nb", withdrawalRequestedAt = cutoff.minusDays(1))
                val all = memberQueryDao.findExpiredWithdrawalRequests(cutoff, null, 10)
                all shouldHaveSize 2

                // When
                val afterCursor = memberQueryDao.findExpiredWithdrawalRequests(cutoff, all.first().id, 10)

                // Then
                afterCursor.map { it.id } shouldBe all.drop(1).map { it.id }
            }
        }
    }
}
