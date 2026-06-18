package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import com.konkuk.ma.domain.matching.entity.table.TargetInfoTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@ContextConfiguration(classes = [TestDatabaseConfig::class, MatchingResultCommandDao::class])
@DatabaseTest
class MatchingResultCommandDaoTest(
    private val matchingResultCommandDao: MatchingResultCommandDao
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(MemberTable, TargetInfoTable, MatchingResultTable)
            insertTestMember("register@example.com")
            insertTestMember("target@example.com")
            insertTestTargetInfo(registerEmail = "register@example.com")
        }

        afterEach {
            SchemaUtils.drop(MatchingResultTable, TargetInfoTable, MemberTable)
        }

        context("deleteExpired") {

            test("만료일이 기준일보다 이전인 데이터를 삭제한다") {
                // Given
                val baseDate = LocalDate.of(2025, 6, 1)
                insertMatchingResult(matchingExpiryDate = LocalDate.of(2025, 5, 31)) // 만료됨
                insertMatchingResult(matchingExpiryDate = LocalDate.of(2025, 5, 30)) // 만료됨

                // When
                val deletedCount = matchingResultCommandDao.deleteExpired(baseDate)

                // Then
                deletedCount shouldBe 2
                MatchingResultTable.selectAll().count() shouldBe 0
            }

            test("만료일이 기준일과 같은 데이터는 삭제하지 않는다") {
                // Given
                val baseDate = LocalDate.of(2025, 6, 1)
                insertMatchingResult(matchingExpiryDate = LocalDate.of(2025, 6, 1))

                // When
                val deletedCount = matchingResultCommandDao.deleteExpired(baseDate)

                // Then
                deletedCount shouldBe 0
                MatchingResultTable.selectAll().count() shouldBe 1
            }

            test("만료일이 기준일보다 이후인 데이터는 삭제하지 않는다") {
                // Given
                val baseDate = LocalDate.of(2025, 6, 1)
                insertMatchingResult(matchingExpiryDate = LocalDate.of(2025, 6, 2))

                // When
                val deletedCount = matchingResultCommandDao.deleteExpired(baseDate)

                // Then
                deletedCount shouldBe 0
                MatchingResultTable.selectAll().count() shouldBe 1
            }

            test("만료된 데이터만 삭제하고 유효한 데이터는 남긴다") {
                // Given
                val baseDate = LocalDate.of(2025, 6, 1)
                insertMatchingResult(matchingExpiryDate = LocalDate.of(2025, 5, 15)) // 만료됨
                insertMatchingResult(matchingExpiryDate = LocalDate.of(2025, 7, 1))  // 유효

                // When
                val deletedCount = matchingResultCommandDao.deleteExpired(baseDate)

                // Then
                deletedCount shouldBe 1
                MatchingResultTable.selectAll().count() shouldBe 1
            }

            test("삭제할 데이터가 없으면 0을 반환한다") {
                // Given
                val baseDate = LocalDate.of(2025, 6, 1)

                // When
                val deletedCount = matchingResultCommandDao.deleteExpired(baseDate)

                // Then
                deletedCount shouldBe 0
            }

            test("excluded=true인 만료된 데이터는 삭제하지 않는다") {
                // Given
                val baseDate = LocalDate.of(2025, 6, 1)
                insertMatchingResult(matchingExpiryDate = LocalDate.of(2025, 5, 31), excluded = true)

                // When
                val deletedCount = matchingResultCommandDao.deleteExpired(baseDate)

                // Then
                deletedCount shouldBe 0
                MatchingResultTable.selectAll().count() shouldBe 1
            }

            test("excluded=false인 만료된 데이터만 삭제한다") {
                // Given
                val baseDate = LocalDate.of(2025, 6, 1)
                insertMatchingResult(matchingExpiryDate = LocalDate.of(2025, 5, 31), excluded = false)
                insertMatchingResult(matchingExpiryDate = LocalDate.of(2025, 5, 30), excluded = true)

                // When
                val deletedCount = matchingResultCommandDao.deleteExpired(baseDate)

                // Then
                deletedCount shouldBe 1
                MatchingResultTable.selectAll().count() shouldBe 1
            }
        }

        context("deleteExcludedExpired") {

            test("excluded=true이고 만료일이 기준일보다 이전인 데이터를 삭제한다") {
                // Given
                val baseDate = LocalDate.of(2025, 6, 1)
                insertMatchingResult(matchingExpiryDate = LocalDate.of(2025, 5, 31), excluded = true)
                insertMatchingResult(matchingExpiryDate = LocalDate.of(2025, 5, 30), excluded = true)

                // When
                val deletedCount = matchingResultCommandDao.deleteExcludedExpired(baseDate)

                // Then
                deletedCount shouldBe 2
                MatchingResultTable.selectAll().count() shouldBe 0
            }

            test("excluded=false인 만료된 데이터는 삭제하지 않는다") {
                // Given
                val baseDate = LocalDate.of(2025, 6, 1)
                insertMatchingResult(matchingExpiryDate = LocalDate.of(2025, 5, 31), excluded = false)

                // When
                val deletedCount = matchingResultCommandDao.deleteExcludedExpired(baseDate)

                // Then
                deletedCount shouldBe 0
                MatchingResultTable.selectAll().count() shouldBe 1
            }

            test("excluded=true이지만 만료일이 기준일 이후인 데이터는 삭제하지 않는다") {
                // Given
                val baseDate = LocalDate.of(2025, 6, 1)
                insertMatchingResult(matchingExpiryDate = LocalDate.of(2025, 6, 2), excluded = true)

                // When
                val deletedCount = matchingResultCommandDao.deleteExcludedExpired(baseDate)

                // Then
                deletedCount shouldBe 0
                MatchingResultTable.selectAll().count() shouldBe 1
            }

            test("삭제할 데이터가 없으면 0을 반환한다") {
                // Given
                val baseDate = LocalDate.of(2025, 6, 1)

                // When
                val deletedCount = matchingResultCommandDao.deleteExcludedExpired(baseDate)

                // Then
                deletedCount shouldBe 0
            }
        }

        context("deleteByRegister") {

            test("회원이 register인 매칭을 soft delete 한다") {
                // Given
                insertMatchingResult()

                // When
                matchingResultCommandDao.deleteByRegister("register@example.com")

                // Then
                MatchingResultTable.selectAll().first()[MatchingResultTable.deleted] shouldBe true
            }

            test("회원이 target일 뿐이면 삭제하지 않는다") {
                // Given
                insertMatchingResult()

                // When
                matchingResultCommandDao.deleteByRegister("target@example.com")

                // Then
                MatchingResultTable.selectAll().first()[MatchingResultTable.deleted] shouldBe false
            }

            test("매칭과 무관한 회원이면 삭제하지 않는다") {
                // Given
                insertMatchingResult()

                // When
                matchingResultCommandDao.deleteByRegister("other@example.com")

                // Then
                MatchingResultTable.selectAll().first()[MatchingResultTable.deleted] shouldBe false
            }
        }

        context("anonymizeTarget") {

            test("회원이 target인 매칭의 targetEmail을 익명 이메일로 교체한다") {
                // Given
                insertMatchingResult()
                insertTestMember("withdrawn_1@deleted.local")

                // When
                matchingResultCommandDao.anonymizeTarget("target@example.com", "withdrawn_1@deleted.local")

                // Then
                MatchingResultTable.selectAll().first()[MatchingResultTable.targetEmail] shouldBe "withdrawn_1@deleted.local"
            }

            test("회원이 register일 뿐이면 targetEmail을 바꾸지 않는다") {
                // Given
                insertMatchingResult()

                // When
                matchingResultCommandDao.anonymizeTarget("register@example.com", "withdrawn_1@deleted.local")

                // Then
                MatchingResultTable.selectAll().first()[MatchingResultTable.targetEmail] shouldBe "target@example.com"
            }

            test("이미 삭제된 매칭은 익명화하지 않는다") {
                // Given
                insertMatchingResult()
                matchingResultCommandDao.deleteByRegister("register@example.com")

                // When
                matchingResultCommandDao.anonymizeTarget("target@example.com", "withdrawn_1@deleted.local")

                // Then
                MatchingResultTable.selectAll().first()[MatchingResultTable.targetEmail] shouldBe "target@example.com"
            }
        }
    }

    private fun insertTestMember(email: String) {
        MemberTable.insert {
            it[MemberTable.email] = email
            it[password] = "password123"
            it[nickname] = "nickname_$email"
            it[gender] = "MALE"
            it[phoneNumber] = "010-1234-5678"
            it[name] = "테스트"
            it[birthDate] = LocalDate.of(2000, 1, 1)
            it[region] = "서울"
        }
    }

    private fun insertTestTargetInfo(registerEmail: String) {
        TargetInfoTable.insert {
            it[TargetInfoTable.registerEmail] = registerEmail
            it[name] = "타겟이름"
            it[targetGender] = "FEMALE"
        }
    }

    private fun insertMatchingResult(
        matchingExpiryDate: LocalDate = LocalDate.now().plusDays(210),
        excluded: Boolean = false
    ) {
        val targetInfoId = TargetInfoTable.selectAll().first()[TargetInfoTable.id].value
        MatchingResultTable.insert {
            it[registerEmail] = "register@example.com"
            it[MatchingResultTable.targetInfoId] = targetInfoId
            it[targetEmail] = "target@example.com"
            it[middleNumberMatched] = true
            it[lastNumberMatched] = true
            it[yearMatched] = true
            it[monthMatched] = true
            it[dayMatched] = true
            it[regionMatched] = true
            it[showingExpiryDate] = LocalDateTime.now().plusDays(30)
            it[MatchingResultTable.matchingExpiryDate] = matchingExpiryDate
            it[MatchingResultTable.excluded] = excluded
        }
    }
}
