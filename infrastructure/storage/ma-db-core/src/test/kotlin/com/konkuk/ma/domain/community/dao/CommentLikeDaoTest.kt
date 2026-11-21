package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.exceptions.ExposedSQLException
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

            test("좋아요를 저장한다") {
                commentLikeDao.save(1L, 100L)

                CommentLikeTable.selectAll().count() shouldBe 1
            }

            test("같은 회원이 같은 댓글에 중복 저장하면 유니크 제약이 거부한다") {
                commentLikeDao.save(1L, 100L)

                shouldThrow<ExposedSQLException> {
                    commentLikeDao.save(1L, 100L)
                }
                CommentLikeTable.selectAll().count() shouldBe 1
            }
        }

        context("exists") {

            test("좋아요가 존재하면 true를 반환한다") {
                commentLikeDao.save(1L, 100L)

                commentLikeDao.exists(1L, 100L) shouldBe true
            }

            test("좋아요가 없으면 false를 반환한다") {
                commentLikeDao.exists(1L, 100L) shouldBe false
            }

            test("다른 회원의 좋아요는 존재로 판단하지 않는다") {
                commentLikeDao.save(1L, 100L)

                commentLikeDao.exists(1L, 999L) shouldBe false
            }
        }

        context("delete") {

            test("좋아요를 삭제한다") {
                commentLikeDao.save(1L, 100L)

                commentLikeDao.delete(1L, 100L)

                CommentLikeTable.selectAll().count() shouldBe 0
            }

            test("존재하지 않는 좋아요 삭제 시 아무 일도 일어나지 않는다") {
                commentLikeDao.delete(999L, 999L)

                CommentLikeTable.selectAll().count() shouldBe 0
            }
        }

        context("count(commentId): 단건 집계") {

            test("좋아요 N개가 저장된 댓글의 좋아요 수를 반환한다") {
                commentLikeDao.save(1L, 101L)
                commentLikeDao.save(1L, 102L)
                commentLikeDao.save(1L, 103L)

                val count = commentLikeDao.count(1L)

                count shouldBe 3
            }

            test("좋아요가 없는 댓글의 좋아요 수는 0이다") {
                val count = commentLikeDao.count(1L)

                count shouldBe 0
            }

            test("soft delete된 좋아요는 단건 집계에서 제외된다") {
                commentLikeDao.save(1L, 101L)
                insertDeletedLike(commentId = 1L, memberId = 200L)

                val count = commentLikeDao.count(1L)

                count shouldBe 1
            }
        }

        context("count(commentIds): 다건 집계") {

            test("여러 댓글의 좋아요 수를 한 번에 정확히 집계한다") {
                commentLikeDao.save(1L, 101L)
                commentLikeDao.save(1L, 102L)
                commentLikeDao.save(2L, 103L)

                val counts = commentLikeDao.count(listOf(1L, 2L))

                counts[1L] shouldBe 2
                counts[2L] shouldBe 1
            }

            test("좋아요가 0인 댓글은 결과 맵에 포함되지 않는다") {
                commentLikeDao.save(1L, 101L)

                val counts = commentLikeDao.count(listOf(1L, 3L))

                counts[1L] shouldBe 1
                counts.containsKey(3L) shouldBe false
            }

            test("빈 ids를 입력하면 쿼리 없이 빈 맵을 반환한다") {
                commentLikeDao.save(1L, 101L)

                val counts = commentLikeDao.count(emptyList())

                counts shouldBe emptyMap<Long, Int>()
            }

            test("soft delete된 좋아요는 다건 집계에서 제외된다") {
                commentLikeDao.save(1L, 101L)
                insertDeletedLike(commentId = 1L, memberId = 200L)
                commentLikeDao.save(2L, 102L)

                val counts = commentLikeDao.count(listOf(1L, 2L))

                counts[1L] shouldBe 1
                counts[2L] shouldBe 1
            }
        }
    }

    private fun insertPost(authorId: Long = 1L) {
        PostTable.insert {
            it[PostTable.authorId] = authorId
            it[category] = "CHEER"
            it[title] = "테스트 게시글"
            it[content] = "내용"
        }
    }

    private fun insertComment(authorId: Long = 2L) {
        CommentTable.insert {
            it[postId] = 1L
            it[CommentTable.authorId] = authorId
            it[content] = "테스트 댓글"
        }
    }

    private fun insertDeletedLike(
        commentId: Long,
        memberId: Long,
    ) {
        CommentLikeTable.insert {
            it[CommentLikeTable.commentId] = commentId
            it[CommentLikeTable.memberId] = memberId
            it[deleted] = true
        }
    }
}
