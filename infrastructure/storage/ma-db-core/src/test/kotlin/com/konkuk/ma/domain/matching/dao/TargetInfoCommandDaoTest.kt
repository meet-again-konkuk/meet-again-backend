package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.matching.entity.table.TargetInfoTable
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.entity.table.MemberTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

@ContextConfiguration(classes = [TestDatabaseConfig::class, TargetInfoCommandDao::class])
@DatabaseTest
class TargetInfoCommandDaoTest(
    private val targetInfoCommandDao: TargetInfoCommandDao
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

        context("save") {

            test("모든 필드가 채워진 NewTargetInfo를 저장하고 ID를 반환한다") {
                // Given
                val newTargetInfo = NewTargetInfo(
                    registerEmail = "test@example.com",
                    targetName = "타겟이름",
                    middleNumber = com.konkuk.ma.domain.member.domain.FourDigit("1234"),
                    lastNumber = com.konkuk.ma.domain.member.domain.FourDigit("5678"),
                    year = com.konkuk.ma.domain.common.domain.date.Year(1995),
                    month = com.konkuk.ma.domain.common.domain.date.Month(3),
                    day = com.konkuk.ma.domain.common.domain.date.Day(15),
                    region = com.konkuk.ma.domain.member.domain.Region.SEOUL
                )

                // When
                val id = targetInfoCommandDao.save(newTargetInfo, Gender.FEMALE)

                // Then
                id shouldBeGreaterThan 0L
                TargetInfoTable.selectAll().count() shouldBe 1
            }

            test("nullable 필드가 null인 NewTargetInfo를 저장한다") {
                // Given
                val newTargetInfo = NewTargetInfo(
                    registerEmail = "test@example.com",
                    targetName = "타겟이름",
                    middleNumber = null,
                    lastNumber = null,
                    year = null,
                    month = null,
                    day = null,
                    region = null
                )

                // When
                val id = targetInfoCommandDao.save(newTargetInfo, Gender.MALE)

                // Then
                id shouldBeGreaterThan 0L
                val row = TargetInfoTable.selectAll().first()
                row[TargetInfoTable.middleNumber] shouldBe null
                row[TargetInfoTable.lastNumber] shouldBe null
                row[TargetInfoTable.year] shouldBe null
                row[TargetInfoTable.month] shouldBe null
                row[TargetInfoTable.day] shouldBe null
                row[TargetInfoTable.region] shouldBe null
            }

            test("여러 건을 저장하면 각각 다른 ID를 반환한다") {
                // Given
                val newTargetInfo1 = createNewTargetInfo(targetName = "타겟1")
                val newTargetInfo2 = createNewTargetInfo(targetName = "타겟2")

                // When
                val id1 = targetInfoCommandDao.save(newTargetInfo1, Gender.FEMALE)
                val id2 = targetInfoCommandDao.save(newTargetInfo2, Gender.MALE)

                // Then
                id1 shouldBeGreaterThan 0L
                id2 shouldBeGreaterThan id1
                TargetInfoTable.selectAll().count() shouldBe 2
            }
        }
    }

    private fun insertMember(email: String = "test@example.com") {
        MemberTable.insert {
            it[MemberTable.email] = email
            it[password] = "password123"
            it[nickname] = "nickname"
            it[gender] = "MALE"
            it[phoneNumber] = "01012345678"
            it[name] = "테스트"
            it[birthDate] = LocalDate.of(1990, 1, 1)
            it[region] = "SEOUL"
        }
    }

    private fun createNewTargetInfo(
        registerEmail: String = "test@example.com",
        targetName: String = "타겟이름",
    ): NewTargetInfo {
        return NewTargetInfo(
            registerEmail = registerEmail,
            targetName = targetName,
            middleNumber = null,
            lastNumber = null,
            year = null,
            month = null,
            day = null,
            region = null
        )
    }
}
