package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, CommentQueryDao::class])
@DatabaseTest
class CommentQueryDaoTest(
    private val commentQueryDao: CommentQueryDao
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

        context("findOne") {

            test("ID로 댓글을 조회한다") {
                // Given
                insertComment(content = "테스트 댓글")
                val commentId = CommentTable.selectAll().first()[CommentTable.id].value

                // When
                val result = commentQueryDao.findOne(commentId)

                // Then
                result shouldNotBe null
                result!!.id shouldBe commentId
                result.content shouldBe "테스트 댓글"
            }

            test("parentCommentId가 있는 대댓글을 조회한다") {
                // Given
                insertComment(content = "부모 댓글")
                val parentId = CommentTable.selectAll().first()[CommentTable.id].value
                insertComment(content = "대댓글", parentCommentId = parentId)
                val childId = CommentTable.selectAll().toList().last()[CommentTable.id].value

                // When
                val result = commentQueryDao.findOne(childId)

                // Then
                result shouldNotBe null
                result!!.parentCommentId shouldBe parentId
            }

            test("deleted가 true인 댓글은 조회되지 않는다") {
                // Given
                insertComment()
                val commentId = CommentTable.selectAll().first()[CommentTable.id].value
                CommentTable.update({ CommentTable.id eq commentId }) {
                    it[deleted] = true
                }

                // When
                val result = commentQueryDao.findOne(commentId)

                // Then
                result shouldBe null
            }

            test("존재하지 않는 ID로 조회하면 null을 반환한다") {
                // When
                val result = commentQueryDao.findOne(9999L)

                // Then
                result shouldBe null
            }
        }
    }

    private fun insertPost(
        authorEmail: String = "author@example.com",
    ) {
        PostTable.insert {
            it[PostTable.authorEmail] = authorEmail
            it[category] = "CHEER"
            it[title] = "테스트 게시글"
            it[content] = "내용"
        }
    }

    private fun insertComment(
        content: String = "테스트 댓글",
        parentCommentId: Long? = null,
    ) {
        val postId = PostTable.selectAll().first()[PostTable.id].value
        CommentTable.insert {
            it[CommentTable.postId] = postId
            it[authorEmail] = "commenter@example.com"
            it[CommentTable.content] = content
            it[CommentTable.parentCommentId] = parentCommentId
        }
    }
}
