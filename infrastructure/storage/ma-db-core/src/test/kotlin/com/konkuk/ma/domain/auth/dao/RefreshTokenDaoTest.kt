package com.konkuk.ma.domain.auth.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.exception.EntityNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@ContextConfiguration(classes = [TestDatabaseConfig::class, RefreshTokenDao::class])
@DatabaseTest
class RefreshTokenDaoTest(
    private val refreshTokenDao: RefreshTokenDao
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(RefreshTokenTable)
        }

        afterEach {
            SchemaUtils.drop(RefreshTokenTable)
        }

        context("save") {

            test("RefreshToken을 저장한다") {
                // Given
                val refreshToken = createRefreshToken()

                // When
                refreshTokenDao.save(refreshToken)

                // Then
                RefreshTokenTable.selectAll().count() shouldBe 1
                val row = RefreshTokenTable.selectAll().first()
                row[RefreshTokenTable.email] shouldBe refreshToken.email
                row[RefreshTokenTable.token] shouldBe refreshToken.token
            }
        }

        context("delete") {

            test("해당 이메일의 RefreshToken을 삭제한다") {
                // Given
                val email = "user@example.com"
                insertRefreshToken(email = email)

                // When
                refreshTokenDao.delete(email)

                // Then
                RefreshTokenTable.selectAll().count() shouldBe 0
            }

            test("다른 이메일의 RefreshToken은 삭제하지 않는다") {
                // Given
                insertRefreshToken(email = "user1@example.com")
                insertRefreshToken(email = "user2@example.com")

                // When
                refreshTokenDao.delete("user1@example.com")

                // Then
                RefreshTokenTable.selectAll().count() shouldBe 1
                RefreshTokenTable.selectAll().first()[RefreshTokenTable.email] shouldBe "user2@example.com"
            }

            test("존재하지 않는 이메일로 삭제해도 예외가 발생하지 않는다") {
                // When
                refreshTokenDao.delete("nobody@example.com")

                // Then
                RefreshTokenTable.selectAll().count() shouldBe 0
            }
        }

        context("findOne") {

            test("이메일로 RefreshToken을 조회한다") {
                // Given
                val email = "user@example.com"
                val token = "test-token-value"
                insertRefreshToken(email = email, token = token)

                // When
                val result = refreshTokenDao.findOne(email)

                // Then
                result.email shouldBe email
                result.token shouldBe token
            }

            test("존재하지 않는 이메일로 조회하면 EntityNotFoundException이 발생한다") {
                // When & Then
                val exception = shouldThrow<EntityNotFoundException> {
                    refreshTokenDao.findOne("nobody@example.com")
                }
                exception.message shouldContain "RefreshToken"
            }
        }
    }

    private fun createRefreshToken(
        email: String = "user@example.com",
        token: String = "test-token-value",
    ): RefreshToken {
        return RefreshToken(
            email = email,
            expirationDate = LocalDateTime.now().plusDays(7),
            token = token
        )
    }

    private fun insertRefreshToken(
        email: String = "user@example.com",
        token: String = "test-token-value",
    ) {
        RefreshTokenTable.insert {
            it[RefreshTokenTable.email] = email
            it[RefreshTokenTable.token] = token
            it[expirationDate] = LocalDateTime.now().plusDays(7)
        }
    }
}
