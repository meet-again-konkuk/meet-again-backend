package com.konkuk.ma.domain.notification.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.notification.entity.table.NotificationTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, NotificationQueryDao::class])
@DatabaseTest
class NotificationQueryDaoTest(
    private val notificationQueryDao: NotificationQueryDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(NotificationTable)
        }

        afterEach {
            SchemaUtils.drop(NotificationTable)
        }

        context("findByRecipient") {

            test("수신자의 알림을 id 내림차순으로 조회한다") {
                // Given
                val recipientId = 1L
                repeat(3) { insertNotification(recipientId = recipientId) }

                // When
                val result = notificationQueryDao.findByRecipient(recipientId, CursorIdCondition.of(cursorId = null, size = 10))

                // Then
                result.data shouldHaveSize 3
                result.data[0].id shouldBe result.data.maxOf { it.id }
            }

            test("cursorId보다 작은 id의 알림만 조회한다") {
                // Given
                val recipientId = 1L
                repeat(5) { insertNotification(recipientId = recipientId) }
                val all = notificationQueryDao.findByRecipient(recipientId, CursorIdCondition.of(cursorId = null, size = 10))
                val cursorId = all.data[1].id // 두 번째로 큰 id

                // When
                val result = notificationQueryDao.findByRecipient(recipientId, CursorIdCondition.of(cursorId = cursorId, size = 10))

                // Then
                result.data.all { it.id < cursorId }.shouldBeTrue()
            }

            test("soft delete된 알림은 조회되지 않는다") {
                // Given
                val recipientId = 1L
                insertNotification(recipientId = recipientId)
                insertNotification(recipientId = recipientId, deleted = true)

                // When
                val result = notificationQueryDao.findByRecipient(recipientId, CursorIdCondition.of(cursorId = null, size = 10))

                // Then
                result.data shouldHaveSize 1
            }

            test("다른 수신자의 알림은 조회되지 않는다") {
                // Given
                val recipientId = 1L
                insertNotification(recipientId = recipientId)
                insertNotification(recipientId = 2L)

                // When
                val result = notificationQueryDao.findByRecipient(recipientId, CursorIdCondition.of(cursorId = null, size = 10))

                // Then
                result.data shouldHaveSize 1
                result.data[0].recipientId shouldBe recipientId
            }

            test("size만큼 채워지고 다음 페이지가 있으면 hasNext가 true다") {
                // Given
                val recipientId = 1L
                repeat(3) { insertNotification(recipientId = recipientId) }

                // When
                val result = notificationQueryDao.findByRecipient(recipientId, CursorIdCondition.of(cursorId = null, size = 2))

                // Then
                result.data shouldHaveSize 2
                result.hasNext.shouldBeTrue()
                result.nextCursorId shouldBe result.data.last().id
            }

            test("다음 페이지가 없으면 hasNext가 false이고 nextCursorId가 null이다") {
                // Given
                val recipientId = 1L
                repeat(2) { insertNotification(recipientId = recipientId) }

                // When
                val result = notificationQueryDao.findByRecipient(recipientId, CursorIdCondition.of(cursorId = null, size = 5))

                // Then
                result.data shouldHaveSize 2
                result.hasNext.shouldBeFalse()
                result.nextCursorId shouldBe null
            }
        }

        context("countUnread") {

            test("읽지 않은 알림 개수를 반환하고 읽은 알림은 제외한다") {
                // Given
                val recipientId = 1L
                insertNotification(recipientId = recipientId, read = false)
                insertNotification(recipientId = recipientId, read = false)
                insertNotification(recipientId = recipientId, read = true)

                // When
                val count = notificationQueryDao.countUnread(recipientId)

                // Then
                count shouldBe 2
            }

            test("soft delete된 알림은 개수에서 제외된다") {
                // Given
                val recipientId = 1L
                insertNotification(recipientId = recipientId, read = false)
                insertNotification(recipientId = recipientId, read = false, deleted = true)

                // When
                val count = notificationQueryDao.countUnread(recipientId)

                // Then
                count shouldBe 1
            }

            test("안읽은 알림이 없으면 0을 반환한다") {
                // Given
                val recipientId = 1L
                insertNotification(recipientId = recipientId, read = true)

                // When
                val count = notificationQueryDao.countUnread(recipientId)

                // Then
                count shouldBe 0
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
}
