package com.konkuk.ma.domain.notification.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.notification.domain.NewNotification
import com.konkuk.ma.domain.notification.domain.NotificationType
import com.konkuk.ma.domain.notification.entity.table.NotificationTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, NotificationCommandDao::class])
@DatabaseTest
class NotificationCommandDaoTest(
    private val notificationCommandDao: NotificationCommandDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(NotificationTable)
        }

        afterEach {
            SchemaUtils.drop(NotificationTable)
        }

        context("save") {

            test("알림을 저장하고 생성된 id를 반환한다") {
                // Given
                val newNotification = NewNotification(
                    recipientId = 1L,
                    type = NotificationType.COMMENT_ON_POST,
                    actorId = 2L,
                    postId = 10L,
                    commentId = 100L,
                )

                // When
                val id = notificationCommandDao.save(newNotification)

                // Then
                id shouldBeGreaterThan 0L
            }

            test("저장된 알림의 필드가 올바르게 저장된다") {
                // Given
                val newNotification = NewNotification(
                    recipientId = 1L,
                    type = NotificationType.REPLY_ON_COMMENT,
                    actorId = 2L,
                    postId = 10L,
                    commentId = 100L,
                )

                // When
                notificationCommandDao.save(newNotification)

                // Then
                val saved = NotificationTable.selectAll().first()
                saved[NotificationTable.recipientId] shouldBe newNotification.recipientId
                saved[NotificationTable.type] shouldBe newNotification.type.name
                saved[NotificationTable.actorId] shouldBe newNotification.actorId
                saved[NotificationTable.postId] shouldBe newNotification.postId
                saved[NotificationTable.commentId] shouldBe newNotification.commentId
                saved[NotificationTable.read] shouldBe false
            }
        }

        context("markAsRead") {

            test("해당 알림을 읽음 처리한다") {
                // Given
                val id = insertNotification(read = false)

                // When
                notificationCommandDao.markAsRead(id)

                // Then
                readFlagOf(id).shouldBeTrue()
            }

            test("지정한 알림만 읽음 처리하고 다른 알림은 그대로 둔다") {
                // Given
                val targetId = insertNotification(read = false)
                val otherId = insertNotification(read = false)

                // When
                notificationCommandDao.markAsRead(targetId)

                // Then
                readFlagOf(targetId).shouldBeTrue()
                readFlagOf(otherId).shouldBeFalse()
            }
        }

        context("markAllAsRead") {

            test("수신자의 모든 안읽은 알림을 읽음 처리한다") {
                // Given
                val recipientId = 1L
                val first = insertNotification(recipientId = recipientId, read = false)
                val second = insertNotification(recipientId = recipientId, read = false)

                // When
                notificationCommandDao.markAllAsRead(recipientId)

                // Then
                readFlagOf(first).shouldBeTrue()
                readFlagOf(second).shouldBeTrue()
            }

            test("다른 수신자의 알림은 읽음 처리하지 않는다") {
                // Given
                val mine = insertNotification(recipientId = 1L, read = false)
                val others = insertNotification(recipientId = 2L, read = false)

                // When
                notificationCommandDao.markAllAsRead(1L)

                // Then
                readFlagOf(mine).shouldBeTrue()
                readFlagOf(others).shouldBeFalse()
            }
        }

        context("deleteByMember") {

            test("수신자가 대상 회원인 알림만 삭제하고 actor로만 참조된 알림은 보존한다") {
                // Given
                val targetMemberId = 1L
                insertNotification(recipientId = targetMemberId, actorId = 2L) // 수신자 → 삭제 대상
                insertNotification(recipientId = 3L, actorId = targetMemberId) // actor로만 참조 → 보존

                // When
                notificationCommandDao.deleteByMember(targetMemberId)

                // Then
                NotificationTable.selectAll().count() shouldBe 1
                NotificationTable.selectAll().first()[NotificationTable.recipientId] shouldBe 3L
            }
        }
    }

    private fun insertNotification(
        recipientId: Long = 1L,
        type: String = "COMMENT_ON_POST",
        actorId: Long = 2L,
        postId: Long = 10L,
        commentId: Long = 100L,
        read: Boolean = false,
        deleted: Boolean = false,
    ): Long {
        return NotificationTable.insertAndGetId {
            it[NotificationTable.recipientId] = recipientId
            it[NotificationTable.type] = type
            it[NotificationTable.actorId] = actorId
            it[NotificationTable.postId] = postId
            it[NotificationTable.commentId] = commentId
            it[NotificationTable.read] = read
            it[NotificationTable.deleted] = deleted
        }.value
    }

    private fun readFlagOf(id: Long): Boolean {
        return NotificationTable.selectAll()
            .first { it[NotificationTable.id].value == id }[NotificationTable.read]
    }
}
