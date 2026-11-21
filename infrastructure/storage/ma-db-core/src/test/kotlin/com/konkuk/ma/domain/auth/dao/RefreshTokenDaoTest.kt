package com.konkuk.ma.domain.auth.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
                val refreshToken = createRefreshToken()

                refreshTokenDao.save(refreshToken)

                RefreshTokenTable.selectAll().count() shouldBe 1
                val row = RefreshTokenTable.selectAll().first()
                row[RefreshTokenTable.memberId] shouldBe refreshToken.memberId
                row[RefreshTokenTable.token] shouldBe refreshToken.token
            }
        }

        context("delete") {

            test("해당 회원의 RefreshToken을 삭제한다") {
                val memberId = 1L
                insertRefreshToken(memberId = memberId)

                refreshTokenDao.delete(memberId)

                RefreshTokenTable.selectAll().count() shouldBe 0
            }

            test("다른 회원의 RefreshToken은 삭제하지 않는다") {
                insertRefreshToken(memberId = 1L)
                insertRefreshToken(memberId = 2L)

                refreshTokenDao.delete(1L)

                RefreshTokenTable.selectAll().count() shouldBe 1
                RefreshTokenTable.selectAll().first()[RefreshTokenTable.memberId] shouldBe 2L
            }

            test("존재하지 않는 회원으로 삭제해도 예외가 발생하지 않는다") {
                refreshTokenDao.delete(999L)

                RefreshTokenTable.selectAll().count() shouldBe 0
            }
        }

        context("findOne") {

            test("회원 식별자로 RefreshToken을 조회한다") {
                val memberId = 1L
                val token = "test-token-value"
                insertRefreshToken(memberId = memberId, token = token)

                val result = refreshTokenDao.findOne(memberId)

                result shouldNotBe null
                result!!.memberId shouldBe memberId
                result.token shouldBe token
            }

            test("존재하지 않는 회원으로 조회하면 null을 반환한다") {
                val result = refreshTokenDao.findOne(999L)

                result shouldBe null
            }
        }
    }

    private fun createRefreshToken(
        memberId: Long = 1L,
        token: String = "test-token-value",
    ): RefreshToken {
        return RefreshToken(
            memberId = memberId,
            expirationDate = LocalDateTime.now().plusDays(7),
            token = token
        )
    }

    private fun insertRefreshToken(
        memberId: Long = 1L,
        token: String = "test-token-value",
    ) {
        RefreshTokenTable.insert {
            it[RefreshTokenTable.memberId] = memberId
            it[RefreshTokenTable.token] = token
            it[expirationDate] = LocalDateTime.now().plusDays(7)
        }
    }
}
