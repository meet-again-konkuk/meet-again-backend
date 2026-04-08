package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.community.entity.table.PostTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, PostQueryDao::class])
@DatabaseTest
class PostQueryDaoTest(
    private val postQueryDao: PostQueryDao
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(PostTable)
        }

        afterEach {
            SchemaUtils.drop(PostTable)
        }

        context("find") {

            test("카테고리와 커서 없이 최신순으로 조회한다") {
                // Given
                repeat(5) { insertPost(title = "게시글$it") }

                // When
                val result = postQueryDao.find(category = null, cursorId = null, size = 3)

                // Then
                result shouldHaveSize 3
                // ID DESC 정렬 확인
                result[0].id shouldBe result.maxOf { it.id }
            }

            test("카테고리로 필터링한다") {
                // Given
                insertPost(category = "CHEER")
                insertPost(category = "COUNSELING")
                insertPost(category = "CHEER")

                // When
                val result = postQueryDao.find(category = "CHEER", cursorId = null, size = 10)

                // Then
                result shouldHaveSize 2
                result.all { it.category.name == "CHEER" }.shouldBeTrue()
            }

            test("cursorId보다 작은 ID의 게시글만 조회한다") {
                // Given
                repeat(5) { insertPost(title = "게시글$it") }
                val allPosts = postQueryDao.find(category = null, cursorId = null, size = 5)
                val cursorId = allPosts[1].id // 두 번째로 큰 ID

                // When
                val result = postQueryDao.find(category = null, cursorId = cursorId, size = 10)

                // Then
                result.all { it.id < cursorId }.shouldBeTrue()
            }

            test("deleted가 true인 게시글은 조회되지 않는다") {
                // Given
                insertPost()
                val postId = PostTable.selectAll().first()[PostTable.id].value
                PostTable.update({ PostTable.id eq postId }) {
                    it[deleted] = true
                }

                // When
                val result = postQueryDao.find(category = null, cursorId = null, size = 10)

                // Then
                result shouldHaveSize 0
            }

            test("데이터가 없으면 빈 리스트를 반환한다") {
                // When
                val result = postQueryDao.find(category = null, cursorId = null, size = 10)

                // Then
                result shouldHaveSize 0
            }

            test("size보다 데이터가 적으면 있는 만큼만 반환한다") {
                // Given
                insertPost()

                // When
                val result = postQueryDao.find(category = null, cursorId = null, size = 10)

                // Then
                result shouldHaveSize 1
            }
        }

        context("findOne") {

            test("ID로 게시글을 조회한다") {
                // Given
                insertPost(title = "테스트 게시글")
                val postId = PostTable.selectAll().first()[PostTable.id].value

                // When
                val result = postQueryDao.findOne(postId)

                // Then
                result shouldNotBe null
                result!!.id shouldBe postId
                result.title shouldBe "테스트 게시글"
            }

            test("deleted가 true인 게시글은 조회되지 않는다") {
                // Given
                insertPost()
                val postId = PostTable.selectAll().first()[PostTable.id].value
                PostTable.update({ PostTable.id eq postId }) {
                    it[deleted] = true
                }

                // When
                val result = postQueryDao.findOne(postId)

                // Then
                result shouldBe null
            }

            test("존재하지 않는 ID로 조회하면 null을 반환한다") {
                // When
                val result = postQueryDao.findOne(9999L)

                // Then
                result shouldBe null
            }
        }

        context("exists") {

            test("게시글이 존재하면 true를 반환한다") {
                // Given
                insertPost()
                val postId = PostTable.selectAll().first()[PostTable.id].value

                // When
                val result = postQueryDao.exists(postId)

                // Then
                result.shouldBeTrue()
            }

            test("deleted가 true인 게시글은 존재하지 않는 것으로 판단한다") {
                // Given
                insertPost()
                val postId = PostTable.selectAll().first()[PostTable.id].value
                PostTable.update({ PostTable.id eq postId }) {
                    it[deleted] = true
                }

                // When
                val result = postQueryDao.exists(postId)

                // Then
                result.shouldBeFalse()
            }

            test("존재하지 않는 ID로 조회하면 false를 반환한다") {
                // When
                val result = postQueryDao.exists(9999L)

                // Then
                result.shouldBeFalse()
            }
        }
    }

    private fun insertPost(
        authorEmail: String = "author@example.com",
        category: String = "CHEER",
        title: String = "테스트 게시글",
        content: String = "내용",
    ) {
        PostTable.insert {
            it[PostTable.authorEmail] = authorEmail
            it[PostTable.category] = category
            it[PostTable.title] = title
            it[PostTable.content] = content
        }
    }
}
