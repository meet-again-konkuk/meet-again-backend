package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.community.entity.table.PostLikeTable
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

@ContextConfiguration(classes = [TestDatabaseConfig::class, PostLikeDao::class])
@DatabaseTest
class PostLikeDaoTest(
    private val postLikeDao: PostLikeDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(PostTable, PostLikeTable)
            insertPost()
        }

        afterEach {
            SchemaUtils.drop(PostLikeTable, PostTable)
        }

        context("save") {

            test("좋아요를 저장한다") {
                postLikeDao.save(1L, 100L)

                PostLikeTable.selectAll().count() shouldBe 1
            }

            test("같은 회원이 같은 게시글에 중복 저장하면 유니크 제약이 거부한다") {
                postLikeDao.save(1L, 100L)

                shouldThrow<ExposedSQLException> {
                    postLikeDao.save(1L, 100L)
                }
                PostLikeTable.selectAll().count() shouldBe 1
            }
        }

        context("exists") {

            test("좋아요가 존재하면 true를 반환한다") {
                postLikeDao.save(1L, 100L)

                postLikeDao.exists(1L, 100L) shouldBe true
            }

            test("좋아요가 없으면 false를 반환한다") {
                postLikeDao.exists(1L, 100L) shouldBe false
            }

            test("다른 회원의 좋아요는 존재로 판단하지 않는다") {
                postLikeDao.save(1L, 100L)

                postLikeDao.exists(1L, 999L) shouldBe false
            }
        }

        context("delete") {

            test("좋아요를 삭제한다") {
                postLikeDao.save(1L, 100L)

                postLikeDao.delete(1L, 100L)

                PostLikeTable.selectAll().count() shouldBe 0
            }

            test("존재하지 않는 좋아요 삭제 시 아무 일도 일어나지 않는다") {
                postLikeDao.delete(999L, 999L)

                PostLikeTable.selectAll().count() shouldBe 0
            }
        }

        context("count(postId): 단건 집계") {

            test("좋아요 N개가 저장된 게시글의 좋아요 수를 반환한다") {
                postLikeDao.save(1L, 101L)
                postLikeDao.save(1L, 102L)
                postLikeDao.save(1L, 103L)

                val count = postLikeDao.count(1L)

                count shouldBe 3
            }

            test("좋아요가 없는 게시글의 좋아요 수는 0이다") {
                val count = postLikeDao.count(1L)

                count shouldBe 0
            }

            test("soft delete된 좋아요는 단건 집계에서 제외된다") {
                postLikeDao.save(1L, 101L)
                insertDeletedLike(postId = 1L, memberId = 200L)

                val count = postLikeDao.count(1L)

                count shouldBe 1
            }
        }

        context("count(postIds): 다건 집계") {

            test("여러 게시글의 좋아요 수를 한 번에 정확히 집계한다") {
                postLikeDao.save(1L, 101L)
                postLikeDao.save(1L, 102L)
                postLikeDao.save(2L, 103L)

                val counts = postLikeDao.count(listOf(1L, 2L))

                counts[1L] shouldBe 2
                counts[2L] shouldBe 1
            }

            test("좋아요가 0인 게시글은 결과 맵에 포함되지 않는다") {
                postLikeDao.save(1L, 101L)

                val counts = postLikeDao.count(listOf(1L, 3L))

                counts[1L] shouldBe 1
                counts.containsKey(3L) shouldBe false
            }

            test("빈 ids를 입력하면 쿼리 없이 빈 맵을 반환한다") {
                postLikeDao.save(1L, 101L)

                val counts = postLikeDao.count(emptyList())

                counts shouldBe emptyMap<Long, Int>()
            }

            test("soft delete된 좋아요는 다건 집계에서 제외된다") {
                postLikeDao.save(1L, 101L)
                insertDeletedLike(postId = 1L, memberId = 200L)
                postLikeDao.save(2L, 102L)

                val counts = postLikeDao.count(listOf(1L, 2L))

                counts[1L] shouldBe 1
                counts[2L] shouldBe 1
            }
        }
    }

    private fun insertPost(
        authorId: Long = 1L,
    ) {
        PostTable.insert {
            it[PostTable.authorId] = authorId
            it[category] = "CHEER"
            it[title] = "테스트 게시글"
            it[content] = "내용"
        }
    }

    private fun insertDeletedLike(
        postId: Long,
        memberId: Long,
    ) {
        PostLikeTable.insert {
            it[PostLikeTable.postId] = postId
            it[PostLikeTable.memberId] = memberId
            it[deleted] = true
        }
    }
}
