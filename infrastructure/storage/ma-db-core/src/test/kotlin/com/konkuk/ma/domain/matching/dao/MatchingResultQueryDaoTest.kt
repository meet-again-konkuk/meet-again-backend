package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import com.konkuk.ma.domain.matching.entity.table.TargetInfoTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@ContextConfiguration(classes = [TestDatabaseConfig::class, MatchingResultQueryDao::class])
@DatabaseTest
class MatchingResultQueryDaoTest(
    private val matchingResultQueryDao: MatchingResultQueryDao
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    private var registerId: Long = 0L
    private var targetId: Long = 0L

    init {
        beforeEach {
            SchemaUtils.create(MemberTable, TargetInfoTable, MatchingResultTable)
            registerId = insertMember("register@example.com")
            targetId = insertMember("target@example.com")
            insertTargetInfo()
        }

        afterEach {
            SchemaUtils.drop(MatchingResultTable, TargetInfoTable, MemberTable)
        }

        context("find(targetInfoIds)") {

            test("targetInfoId 목록에 해당하는 매칭 결과를 조회한다") {
                // Given
                val targetInfoId = getTargetInfoId()
                insertMatchingResult()

                // When
                val result = matchingResultQueryDao.find(listOf(targetInfoId))

                // Then
                result shouldHaveSize 1
                result[0].targetInfoId shouldBe targetInfoId
            }

            test("빈 targetInfoIds로 조회하면 빈 리스트를 반환한다") {
                // When
                val result = matchingResultQueryDao.find(emptyList())

                // Then
                result shouldHaveSize 0
            }

            test("일치하는 targetInfoId가 없으면 빈 리스트를 반환한다") {
                // When
                val result = matchingResultQueryDao.find(listOf(9999L))

                // Then
                result shouldHaveSize 0
            }

            test("deleted=true인 매칭 결과는 조회되지 않는다") {
                // Given
                val targetInfoId = getTargetInfoId()
                insertMatchingResult()
                MatchingResultTable.update({ MatchingResultTable.targetInfoId eq targetInfoId }) {
                    it[deleted] = true
                }

                // When
                val result = matchingResultQueryDao.find(listOf(targetInfoId))

                // Then
                result shouldHaveSize 0
            }
        }

        context("find(memberId, excluded)") {

            test("memberId로 excluded=false인 매칭 결과를 조회한다") {
                // Given
                insertMatchingResult(excluded = false)

                // When
                val result = matchingResultQueryDao.find(registerId)

                // Then
                result shouldHaveSize 1
                result[0].registerId shouldBe registerId
                result[0].excluded shouldBe false
            }

            test("excluded=true를 지정하면 excluded 매칭 결과만 조회한다") {
                // Given
                insertMatchingResult(excluded = true)
                insertMatchingResult(excluded = false)

                // When
                val result = matchingResultQueryDao.find(registerId, excluded = true)

                // Then
                result shouldHaveSize 1
                result[0].excluded shouldBe true
            }

            test("deleted=true인 매칭 결과는 조회되지 않는다") {
                // Given
                insertMatchingResult()
                MatchingResultTable.update({ MatchingResultTable.registerId eq registerId }) {
                    it[deleted] = true
                }

                // When
                val result = matchingResultQueryDao.find(registerId)

                // Then
                result shouldHaveSize 0
            }

            test("일치하는 회원이 없으면 빈 리스트를 반환한다") {
                // When
                val result = matchingResultQueryDao.find(9999L)

                // Then
                result shouldHaveSize 0
            }
        }

        context("findOne") {

            test("ID로 매칭 결과를 조회한다") {
                // Given
                insertMatchingResult()
                val id = MatchingResultTable.selectAll().first()[MatchingResultTable.id].value

                // When
                val result = matchingResultQueryDao.findOne(id)

                // Then
                result shouldNotBe null
                result!!.id shouldBe id
            }

            test("deleted=true인 매칭 결과는 조회되지 않는다") {
                // Given
                insertMatchingResult()
                val id = MatchingResultTable.selectAll().first()[MatchingResultTable.id].value
                MatchingResultTable.update({ MatchingResultTable.id eq id }) {
                    it[deleted] = true
                }

                // When
                val result = matchingResultQueryDao.findOne(id)

                // Then
                result shouldBe null
            }

            test("존재하지 않는 ID로 조회하면 null을 반환한다") {
                // When
                val result = matchingResultQueryDao.findOne(9999L)

                // Then
                result shouldBe null
            }
        }

        context("exists") {

            test("해당 targetInfoId의 매칭 결과가 존재하면 true를 반환한다") {
                // Given
                val targetInfoId = getTargetInfoId()
                insertMatchingResult()

                // When
                val result = matchingResultQueryDao.exists(targetInfoId)

                // Then
                result.shouldBeTrue()
            }

            test("해당 targetInfoId의 매칭 결과가 없으면 false를 반환한다") {
                // When
                val result = matchingResultQueryDao.exists(9999L)

                // Then
                result.shouldBeFalse()
            }

            test("deleted=true인 매칭 결과는 존재하지 않는 것으로 판단한다") {
                // Given
                val targetInfoId = getTargetInfoId()
                insertMatchingResult()
                MatchingResultTable.update({ MatchingResultTable.targetInfoId eq targetInfoId }) {
                    it[deleted] = true
                }

                // When
                val result = matchingResultQueryDao.exists(targetInfoId)

                // Then
                result.shouldBeFalse()
            }
        }

        context("findClaimedByTarget") {

            test("targetId로 claimed=true인 매칭 결과를 조회한다") {
                insertMatchingResult(claimed = true)

                val result = matchingResultQueryDao.findClaimedByTarget(targetId)

                result shouldHaveSize 1
                result[0].claimed shouldBe true
            }

            test("claimed=false인 결과는 조회되지 않는다") {
                insertMatchingResult(claimed = false)

                val result = matchingResultQueryDao.findClaimedByTarget(targetId)

                result shouldHaveSize 0
            }

            test("다른 회원이 target인 결과는 조회되지 않는다") {
                insertMatchingResult(claimed = true)

                val result = matchingResultQueryDao.findClaimedByTarget(9999L)

                result shouldHaveSize 0
            }
        }
    }

    private fun insertMember(email: String): Long {
        return MemberTable.insertAndGetId {
            it[MemberTable.email] = email
            it[password] = "password123"
            it[nickname] = "nickname_$email"
            it[gender] = "MALE"
            it[phoneNumber] = "01012345678"
            it[name] = "테스트"
            it[birthDate] = LocalDate.of(2000, 1, 1)
            it[region] = "SEOUL"
        }.value
    }

    private fun insertTargetInfo(registerId: Long = this.registerId) {
        TargetInfoTable.insert {
            it[TargetInfoTable.registerId] = registerId
            it[name] = "타겟이름"
            it[targetGender] = "FEMALE"
        }
    }

    private fun getTargetInfoId(): Long {
        return TargetInfoTable.selectAll().first()[TargetInfoTable.id].value
    }

    private fun insertMatchingResult(
        excluded: Boolean = false,
        claimed: Boolean = false,
        registerId: Long = this.registerId,
        targetId: Long = this.targetId,
    ) {
        val targetInfoId = getTargetInfoId()
        MatchingResultTable.insert {
            it[MatchingResultTable.registerId] = registerId
            it[MatchingResultTable.targetInfoId] = targetInfoId
            it[MatchingResultTable.targetId] = targetId
            it[middleNumberMatched] = true
            it[lastNumberMatched] = true
            it[yearMatched] = true
            it[monthMatched] = true
            it[dayMatched] = true
            it[regionMatched] = true
            it[showingExpiryDate] = LocalDateTime.now().plusDays(30)
            it[matchingExpiryDate] = LocalDate.now().plusDays(210)
            it[MatchingResultTable.excluded] = excluded
            it[MatchingResultTable.claimed] = claimed
        }
    }
}
