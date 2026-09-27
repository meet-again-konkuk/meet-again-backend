package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.community.entity.table.CommentNotificationSettingTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, CommentNotificationSettingDao::class])
@DatabaseTest
class CommentNotificationSettingDaoTest(
    private val commentNotificationSettingDao: CommentNotificationSettingDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(CommentNotificationSettingTable)
        }

        afterEach {
            SchemaUtils.drop(CommentNotificationSettingTable)
        }

        context("optOut") {

            test("opt-out 설정을 저장한다") {
                // When
                commentNotificationSettingDao.optOut(1L, 10L)

                // Then
                CommentNotificationSettingTable.selectAll().count() shouldBe 1
                commentNotificationSettingDao.isOptedOut(1L, 10L).shouldBeTrue()
            }

        }

        context("isOptedOut") {

            test("opt-out 되어 있으면 true를 반환한다") {
                // Given
                commentNotificationSettingDao.optOut(1L, 10L)

                // When & Then
                commentNotificationSettingDao.isOptedOut(1L, 10L).shouldBeTrue()
            }

            test("opt-out 되어 있지 않으면 false를 반환한다") {
                // When & Then
                commentNotificationSettingDao.isOptedOut(1L, 10L).shouldBeFalse()
            }

            test("다른 게시글의 opt-out은 영향을 주지 않는다") {
                // Given
                commentNotificationSettingDao.optOut(1L, 10L)

                // When & Then
                commentNotificationSettingDao.isOptedOut(1L, 99L).shouldBeFalse()
            }
        }

        context("optIn") {

            test("opt-out 설정을 삭제한다") {
                // Given
                commentNotificationSettingDao.optOut(1L, 10L)

                // When
                commentNotificationSettingDao.optIn(1L, 10L)

                // Then
                commentNotificationSettingDao.isOptedOut(1L, 10L).shouldBeFalse()
                CommentNotificationSettingTable.selectAll().count() shouldBe 0
            }

            test("opt-out 설정이 없을 때 opt-in해도 아무 일도 일어나지 않는다") {
                // When
                commentNotificationSettingDao.optIn(1L, 10L)

                // Then
                CommentNotificationSettingTable.selectAll().count() shouldBe 0
            }
        }

        context("deleteByMember") {

            test("회원의 모든 opt-out 설정을 삭제하고 다른 회원의 설정은 보존한다") {
                // Given
                commentNotificationSettingDao.optOut(1L, 10L)
                commentNotificationSettingDao.optOut(1L, 20L)
                commentNotificationSettingDao.optOut(2L, 10L)

                // When
                commentNotificationSettingDao.deleteByMember(1L)

                // Then
                CommentNotificationSettingTable.selectAll().count() shouldBe 1
                commentNotificationSettingDao.isOptedOut(2L, 10L).shouldBeTrue()
            }
        }

        context("유니크 제약") {

            test("같은 회원·게시글로 직접 중복 삽입하면 유니크 제약이 거부한다") {
                // Given
                insertSetting(1L, 10L)

                // When & Then
                shouldThrow<ExposedSQLException> {
                    insertSetting(1L, 10L)
                }
                CommentNotificationSettingTable.selectAll().count() shouldBe 1
            }
        }
    }

    private fun insertSetting(memberId: Long, postId: Long) {
        CommentNotificationSettingTable.insert {
            it[CommentNotificationSettingTable.memberId] = memberId
            it[CommentNotificationSettingTable.postId] = postId
        }
    }
}
