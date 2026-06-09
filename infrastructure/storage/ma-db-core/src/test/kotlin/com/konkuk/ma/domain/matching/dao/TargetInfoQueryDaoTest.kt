package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.matching.entity.table.TargetInfoTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@ContextConfiguration(classes = [TestDatabaseConfig::class, TargetInfoQueryDao::class])
@DatabaseTest
class TargetInfoQueryDaoTest(
    private val targetInfoQueryDao: TargetInfoQueryDao
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(MemberTable, TargetInfoTable)
            insertMember()
        }

        afterEach {
            SchemaUtils.drop(TargetInfoTable, MemberTable)
        }

        context("findNoOffset") {

            test("cursorId가 null이면 처음부터 size만큼 조회한다") {
                // Given
                repeat(5) { insertTargetInfo(name = "타겟$it") }

                // When
                val result = targetInfoQueryDao.findNoOffset(cursorId = null, size = 3)

                // Then
                result shouldHaveSize 3
                result[0].name shouldBe "타겟0"
                result[1].name shouldBe "타겟1"
                result[2].name shouldBe "타겟2"
            }

            test("cursorId를 기준으로 이후 데이터를 조회한다") {
                // Given
                repeat(5) { insertTargetInfo(name = "타겟$it") }
                val allItems = targetInfoQueryDao.findNoOffset(cursorId = null, size = 5)
                val cursorId = allItems[1].id

                // When
                val result = targetInfoQueryDao.findNoOffset(cursorId = cursorId, size = 3)

                // Then
                result shouldHaveSize 3
                result[0].name shouldBe "타겟2"
                result[1].name shouldBe "타겟3"
                result[2].name shouldBe "타겟4"
            }

            test("데이터가 size보다 적으면 있는 만큼만 반환한다") {
                // Given
                insertTargetInfo(name = "타겟0")

                // When
                val result = targetInfoQueryDao.findNoOffset(cursorId = null, size = 10)

                // Then
                result shouldHaveSize 1
            }

            test("데이터가 없으면 빈 리스트를 반환한다") {
                // When
                val result = targetInfoQueryDao.findNoOffset(cursorId = null, size = 10)

                // Then
                result shouldHaveSize 0
            }

            test("cursorId 이후에 데이터가 없으면 빈 리스트를 반환한다") {
                // Given
                repeat(3) { insertTargetInfo(name = "타겟$it") }
                val allItems = targetInfoQueryDao.findNoOffset(cursorId = null, size = 3)
                val lastId = allItems.last().id

                // When
                val result = targetInfoQueryDao.findNoOffset(cursorId = lastId, size = 10)

                // Then
                result shouldHaveSize 0
            }

            test("ID 오름차순으로 정렬되어 반환한다") {
                // Given
                repeat(3) { insertTargetInfo(name = "타겟$it") }

                // When
                val result = targetInfoQueryDao.findNoOffset(cursorId = null, size = 3)

                // Then
                result[0].id shouldBe result.minOf { it.id }
                result[2].id shouldBe result.maxOf { it.id }
            }

            test("탈퇴 신청한 등록 회원의 타겟은 제외되고 활성 회원의 타겟만 조회된다") {
                // Given
                insertMember("active@example.com")
                insertTargetInfo(registerEmail = "active@example.com", name = "활성타겟")
                insertTargetInfo(registerEmail = "test@example.com", name = "탈퇴타겟")
                markWithdrawalPending("test@example.com")

                // When
                val result = targetInfoQueryDao.findNoOffset(cursorId = null, size = 10)

                // Then
                result shouldHaveSize 1
                result[0].name shouldBe "활성타겟"
            }
        }
    }

    private fun insertMember(email: String = "test@example.com") {
        MemberTable.insert {
            it[MemberTable.email] = email
            it[password] = "password123"
            it[nickname] = "nickname_$email"
            it[gender] = "MALE"
            it[phoneNumber] = "01012345678"
            it[MemberTable.name] = "테스트"
            it[birthDate] = LocalDate.of(1990, 1, 1)
            it[region] = "SEOUL"
        }
    }

    private fun markWithdrawalPending(email: String) {
        MemberTable.update({ MemberTable.email eq email }) {
            it[withdrawalRequestedAt] = LocalDateTime.now()
        }
    }

    private fun insertTargetInfo(
        registerEmail: String = "test@example.com",
        name: String = "타겟이름",
    ) {
        TargetInfoTable.insert {
            it[TargetInfoTable.registerEmail] = registerEmail
            it[TargetInfoTable.name] = name
            it[targetGender] = "FEMALE"
        }
    }
}
