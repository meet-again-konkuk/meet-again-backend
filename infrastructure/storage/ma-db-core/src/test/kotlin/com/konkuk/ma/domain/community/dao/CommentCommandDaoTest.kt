package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, CommentCommandDao::class])
@DatabaseTest
class CommentCommandDaoTest(
    private val commentCommandDao: CommentCommandDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(PostTable, CommentTable)
            insertPost()
        }

        afterEach {
            SchemaUtils.drop(CommentTable, PostTable)
        }

        context("increaseLikes") {

            test("좋아요 수를 1 증가시키고 변경 후 카운트를 반환한다") {
                // Given
                val commentId = insertComment()

                // When
                val likeCount = commentCommandDao.increaseLikes(commentId)

                // Then
                likeCount shouldBe 1
            }

            test("여러 번 증가시키면 누적된다") {
                // Given
                val commentId = insertComment()

                // When
                commentCommandDao.increaseLikes(commentId)
                val likeCount = commentCommandDao.increaseLikes(commentId)

                // Then
                likeCount shouldBe 2
            }
        }

        context("decreaseLikes") {

            test("좋아요 수를 1 감소시키고 변경 후 카운트를 반환한다") {
                // Given
                val commentId = insertComment()
                commentCommandDao.increaseLikes(commentId)

                // When
                val likeCount = commentCommandDao.decreaseLikes(commentId)

                // Then
                likeCount shouldBe 0
            }
        }
    }

    private fun insertPost(authorEmail: String = "author@example.com") {
        PostTable.insert {
            it[PostTable.authorEmail] = authorEmail
            it[category] = "CHEER"
            it[title] = "테스트 게시글"
            it[content] = "내용"
        }
    }

    private fun insertComment(authorEmail: String = "author@example.com"): Long {
        return CommentTable.insert {
            it[postId] = 1L
            it[CommentTable.authorEmail] = authorEmail
            it[content] = "테스트 댓글"
        }[CommentTable.id].value
    }
}
