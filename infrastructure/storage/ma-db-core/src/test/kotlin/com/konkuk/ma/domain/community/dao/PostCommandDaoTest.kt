package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.community.entity.table.PostTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [TestDatabaseConfig::class, PostCommandDao::class])
@DatabaseTest
class PostCommandDaoTest(
    private val postCommandDao: PostCommandDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(PostTable)
        }

        afterEach {
            SchemaUtils.drop(PostTable)
        }

        context("increaseLikes") {

            test("좋아요 수를 1 증가시키고 변경 후 카운트를 반환한다") {
                // Given
                val postId = insertPost()

                // When
                val likeCount = postCommandDao.increaseLikes(postId)

                // Then
                likeCount shouldBe 1
            }

            test("여러 번 증가시키면 누적된다") {
                // Given
                val postId = insertPost()

                // When
                postCommandDao.increaseLikes(postId)
                val likeCount = postCommandDao.increaseLikes(postId)

                // Then
                likeCount shouldBe 2
            }
        }

        context("decreaseLikes") {

            test("좋아요 수를 1 감소시키고 변경 후 카운트를 반환한다") {
                // Given
                val postId = insertPost()
                postCommandDao.increaseLikes(postId)

                // When
                val likeCount = postCommandDao.decreaseLikes(postId)

                // Then
                likeCount shouldBe 0
            }
        }
    }

    private fun insertPost(authorEmail: String = "author@example.com"): Long {
        return PostTable.insert {
            it[PostTable.authorEmail] = authorEmail
            it[category] = "CHEER"
            it[title] = "테스트 게시글"
            it[content] = "내용"
        }[PostTable.id].value
    }
}
