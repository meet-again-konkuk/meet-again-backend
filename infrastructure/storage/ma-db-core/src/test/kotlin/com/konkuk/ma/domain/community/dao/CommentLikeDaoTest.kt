package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, CommentLikeDao::class])
@DatabaseTest
class CommentLikeDaoTest(
    private val commentLikeDao: CommentLikeDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(PostTable, CommentTable, CommentLikeTable)
            insertPost()
            insertComment()
        }

        afterEach {
            SchemaUtils.drop(CommentLikeTable, CommentTable, PostTable)
        }

        context("save") {

            test("좋아요를 저장하고 ID를 반환한다") {
                // When
                val id = commentLikeDao.save(1L, "user@example.com")

                // Then
                id shouldBeGreaterThan 0L
                CommentLikeTable.selectAll().count() shouldBe 1
            }
        }

        context("delete") {

            test("좋아요를 삭제한다") {
                // Given
                commentLikeDao.save(1L, "user@example.com")

                // When
                commentLikeDao.delete(1L, "user@example.com")

                // Then
                CommentLikeTable.selectAll().count() shouldBe 0
            }

            test("존재하지 않는 좋아요 삭제 시 아무 일도 일어나지 않는다") {
                // When
                commentLikeDao.delete(999L, "nobody@example.com")

                // Then
                CommentLikeTable.selectAll().count() shouldBe 0
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

    private fun insertComment(authorEmail: String = "author@example.com") {
        CommentTable.insert {
            it[postId] = 1L
            it[CommentTable.authorEmail] = authorEmail
            it[content] = "테스트 댓글"
        }
    }
}
