package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.DatabaseTestConfig
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.member.entity.table.MemberTable
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.insert
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, MemberValidateDao::class])
@DatabaseTest
class MemberValidateDaoTest(
    private val memberValidateDao: MemberValidateDao
) : DatabaseTestConfig() {

    init {
        test("existsByNickname - 닉네임이 존재하는 경우 true를 반환한다") {
            // Given
            val nickname = "testNickname"

            MemberTable.insert {
                it[email] = "test@example.com"
                it[password] = "password123"
                it[MemberTable.nickname] = nickname
                it[phoneNumber] = "010-1234-5678"
            }

            // When
            val result = memberValidateDao.existsByNickname(nickname)

            // Then
            result shouldBe true
        }

        test("existsByNickname - 닉네임이 존재하지 않는 경우 false를 반환한다") {
            // Given
            val nickname = "nonExistentNickname"

            // When
            val result = memberValidateDao.existsByNickname(nickname)

            // Then
            result shouldBe false
        }

        test("existsByEmail - 이메일이 존재하는 경우 true를 반환한다") {
            // Given
            val email = "test@example.com"

            MemberTable.insert {
                it[MemberTable.email] = email
                it[password] = "password123"
                it[nickname] = "testNickname"
                it[phoneNumber] = "010-1234-5678"
            }

            // When
            val result = memberValidateDao.existsByEmail(email)

            // Then
            result shouldBe true
        }

        test("existsByEmail - 이메일이 존재하지 않는 경우 false를 반환한다") {
            // Given
            val email = "nonexistent@example.com"

            // When
            val result = memberValidateDao.existsByEmail(email)

            // Then
            result shouldBe false
        }


        test("existsByEmail - 대소문자가 다른 이메일로 조회해도 정확히 매치되는 경우만 true를 반환한다") {
            // Given
            val email = "Test@Example.com"
            val searchEmail = "test@example.com"

            MemberTable.insert {
                it[MemberTable.email] = email
                it[password] = "password123"
                it[nickname] = "testNickname"
                it[phoneNumber] = "010-1234-5678"
            }

            // When
            val result = memberValidateDao.existsByEmail(searchEmail)

            // Then
            result shouldBe false // 정확히 매치되지 않으므로 false
        }

        test("existsByNickname - 빈 문자열로 검색하는 경우 false를 반환한다") {
            // Given
            val nickname = ""

            // When
            val result = memberValidateDao.existsByNickname(nickname)

            // Then
            result shouldBe false
        }

        test("existsByEmail - 빈 문자열로 검색하는 경우 false를 반환한다") {
            // Given
            val email = ""

            // When
            val result = memberValidateDao.existsByEmail(email)

            // Then
            result shouldBe false
        }
    }
}
