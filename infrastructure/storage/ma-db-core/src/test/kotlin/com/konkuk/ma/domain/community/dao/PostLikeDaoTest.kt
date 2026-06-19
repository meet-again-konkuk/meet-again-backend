package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.community.entity.table.PostLikeTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
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

            test("좋아요를 저장하고 ID를 반환한다") {
                // When
                val id = postLikeDao.save(1L, 100L)

                // Then
                id shouldBeGreaterThan 0L
                PostLikeTable.selectAll().count() shouldBe 1
            }
        }

        context("delete") {

            test("좋아요를 삭제한다") {
                // Given
                postLikeDao.save(1L, 100L)

                // When
                postLikeDao.delete(1L, 100L)

                // Then
                PostLikeTable.selectAll().count() shouldBe 0
            }

            test("존재하지 않는 좋아요 삭제 시 아무 일도 일어나지 않는다") {
                // When
                postLikeDao.delete(999L, 999L)

                // Then
                PostLikeTable.selectAll().count() shouldBe 0
            }
        }

        context("count(postId): 단건 집계") {

            test("좋아요 N개가 저장된 게시글의 좋아요 수를 반환한다") {
                // Given
                postLikeDao.save(1L, 101L)
                postLikeDao.save(1L, 102L)
                postLikeDao.save(1L, 103L)

                // When
                val count = postLikeDao.count(1L)

                // Then
                count shouldBe 3
            }

            test("좋아요가 없는 게시글의 좋아요 수는 0이다") {
                // When
                val count = postLikeDao.count(1L)

                // Then
                count shouldBe 0
            }

            test("soft delete된 좋아요는 단건 집계에서 제외된다") {
                // Given
                postLikeDao.save(1L, 101L)
                insertDeletedLike(postId = 1L, memberId = 200L)

                // When
                val count = postLikeDao.count(1L)

                // Then - 삭제되지 않은 1건만 집계된다
                count shouldBe 1
            }
        }

        context("count(postIds): 다건 집계") {

            test("여러 게시글의 좋아요 수를 한 번에 정확히 집계한다") {
                // Given - 게시글마다 서로 다른 좋아요 수
                postLikeDao.save(1L, 101L)
                postLikeDao.save(1L, 102L)
                postLikeDao.save(2L, 103L)

                // When
                val counts = postLikeDao.count(listOf(1L, 2L))

                // Then
                counts[1L] shouldBe 2
                counts[2L] shouldBe 1
            }

            test("좋아요가 0인 게시글은 결과 맵에 포함되지 않는다") {
                // Given - 1번 게시글만 좋아요, 3번은 좋아요 없음
                postLikeDao.save(1L, 101L)

                // When
                val counts = postLikeDao.count(listOf(1L, 3L))

                // Then - 좋아요 0인 3번은 맵에 없다 (LikeCounts가 0으로 보정)
                counts[1L] shouldBe 1
                counts.containsKey(3L) shouldBe false
            }

            test("빈 ids를 입력하면 쿼리 없이 빈 맵을 반환한다") {
                // Given - 좋아요 행이 존재해도
                postLikeDao.save(1L, 101L)

                // When
                val counts = postLikeDao.count(emptyList())

                // Then
                counts shouldBe emptyMap<Long, Int>()
            }

            test("soft delete된 좋아요는 다건 집계에서 제외된다") {
                // Given - 1번: 활성 1건 + 삭제 1건, 2번: 활성 1건
                postLikeDao.save(1L, 101L)
                insertDeletedLike(postId = 1L, memberId = 200L)
                postLikeDao.save(2L, 102L)

                // When
                val counts = postLikeDao.count(listOf(1L, 2L))

                // Then - 삭제된 행은 빠지고 활성 행만 집계된다
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
