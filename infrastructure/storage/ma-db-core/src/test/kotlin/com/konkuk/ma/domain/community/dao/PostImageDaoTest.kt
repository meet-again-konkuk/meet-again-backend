package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.config.DatabaseTest
import com.konkuk.ma.config.TestDatabaseConfig
import com.konkuk.ma.domain.community.domain.image.NewPostImage
import com.konkuk.ma.domain.community.entity.table.PostImageTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.springframework.test.context.ContextConfiguration

/**
 * 게시글 이미지 DAO 통합 테스트 (MediaDaoTest 미러).
 *
 * PostImageCommandDao(save/softDeleteByPost/delete) 와 PostImageQueryDao(findActiveByPost(s)/findDeletedBefore)
 * 가 실제 H2 위에서 의도한 쿼리대로 동작하는지 검증한다. deletedDate 를 특정 시각으로 제어해야 하는
 * findDeletedBefore 케이스는 softDelete 헬퍼(now 사용) 대신 직접 삽입한다.
 */
@ContextConfiguration(classes = [TestDatabaseConfig::class, PostImageCommandDao::class, PostImageQueryDao::class])
@DatabaseTest
class PostImageDaoTest(
    private val postImageCommandDao: PostImageCommandDao,
    private val postImageQueryDao: PostImageQueryDao,
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            SchemaUtils.create(PostImageTable)
        }

        afterEach {
            SchemaUtils.drop(PostImageTable)
        }

        fun newPostImage(
            postId: Long = 1L,
            storageKey: String = "community/post-image/1/photo.jpg",
            originalFilename: String = "photo.jpg",
            mimeType: String = "image/jpeg",
            fileSize: Long = 2048L,
            thumbnailKey: String? = "community/thumbnail/1/thumb_photo.jpg",
        ) = NewPostImage(
            postId = postId,
            storageKey = storageKey,
            originalFilename = originalFilename,
            mimeType = mimeType,
            fileSize = fileSize,
            thumbnailKey = thumbnailKey,
        )

        // save 는 아직 미구현이므로, 쿼리 검증용 행은 Exposed DSL 로 직접 삽입한다.
        fun insertPostImage(
            postId: Long = 1L,
            storageKey: String = "community/post-image/1/photo.jpg",
            thumbnailKey: String? = "community/thumbnail/1/thumb_photo.jpg",
            deleted: Boolean = false,
            deletedDate: LocalDateTime? = null,
        ): Long = PostImageTable.insertAndGetId {
            it[PostImageTable.postId] = postId
            it[PostImageTable.storageKey] = storageKey
            it[originalFilename] = "photo.jpg"
            it[mimeType] = "image/jpeg"
            it[fileSize] = 2048L
            it[PostImageTable.thumbnailKey] = thumbnailKey
            it[PostImageTable.deleted] = deleted
            it[PostImageTable.deletedDate] = deletedDate
        }.value

        context("save") {

            test("이미지를 저장하고 ID 를 반환한다") {
                val id = postImageCommandDao.save(newPostImage())

                id shouldNotBe null
                id shouldBe 1L
            }

            test("저장한 값이 그대로 적재되어 조회 시 반영된다") {
                val image = newPostImage()

                val id = postImageCommandDao.save(image)

                val found = postImageQueryDao.findActiveByPosts(listOf(image.postId)).first()
                found.id shouldBe id
                found.storageKey shouldBe image.storageKey
                found.originalFilename shouldBe image.originalFilename
                found.mimeType shouldBe image.mimeType
                found.fileSize shouldBe image.fileSize
                found.thumbnailKey shouldBe image.thumbnailKey
            }
        }

        context("findOneActiveOrNull") {

            test("해당 게시글의 active 이미지를 반환한다") {
                val postId = 3L
                insertPostImage(postId = postId)

                val found = postImageQueryDao.findOneActiveOrNull(postId)

                found.shouldNotBeNull()
                found.postId shouldBe postId
            }

            test("이미지가 없으면 null 을 반환한다") {
                postImageQueryDao.findOneActiveOrNull(999L) shouldBe null
            }

            test("soft-deleted 이미지만 있으면 null 을 반환한다") {
                val postId = 4L
                insertPostImage(postId = postId, deleted = true, deletedDate = LocalDateTime.now())

                postImageQueryDao.findOneActiveOrNull(postId) shouldBe null
            }
        }

        context("findActiveByPosts") {

            test("빈 목록이면 빈 리스트를 반환한다") {
                insertPostImage(postId = 1L)

                postImageQueryDao.findActiveByPosts(emptyList()).shouldBeEmpty()
            }

            test("여러 게시글의 이미지를 벌크 조회한다") {
                insertPostImage(postId = 1L)
                insertPostImage(postId = 2L)
                insertPostImage(postId = 3L)

                val found = postImageQueryDao.findActiveByPosts(listOf(1L, 2L))

                found shouldHaveSize 2
                found.map { it.postId }.toSet() shouldBe setOf(1L, 2L)
            }

            test("soft-deleted 이미지는 벌크 조회에서 제외된다") {
                insertPostImage(postId = 1L)
                insertPostImage(postId = 2L, deleted = true, deletedDate = LocalDateTime.now())

                val found = postImageQueryDao.findActiveByPosts(listOf(1L, 2L))

                found shouldHaveSize 1
                found.first().postId shouldBe 1L
            }
        }

        context("softDeleteByPost") {

            test("deleted=true·deletedBy=memberId 가 채워지고 이후 조회에서 제외된다") {
                val postId = 1L
                val memberId = 7L
                insertPostImage(postId = postId)

                postImageCommandDao.softDeleteByPost(postId, memberId)

                val row = PostImageTable.selectAll()
                    .where { PostImageTable.postId eq postId }
                    .single()
                row[PostImageTable.deleted] shouldBe true
                row[PostImageTable.deletedBy] shouldBe memberId.toString()
                row[PostImageTable.deletedDate] shouldNotBe null

                postImageQueryDao.findOneActiveOrNull(postId) shouldBe null
            }

            test("대상 게시글의 이미지만 soft delete 하고 다른 게시글 이미지는 남긴다") {
                insertPostImage(postId = 1L)
                insertPostImage(postId = 2L)

                postImageCommandDao.softDeleteByPost(postId = 1L, memberId = 7L)

                postImageQueryDao.findOneActiveOrNull(1L) shouldBe null
                postImageQueryDao.findOneActiveOrNull(2L).shouldNotBeNull()
            }
        }

        context("findDeletedBefore") {

            val cutoff = LocalDateTime.of(2026, 6, 10, 0, 0)
            val beforeCutoff = LocalDateTime.of(2026, 6, 1, 0, 0)
            val afterCutoff = LocalDateTime.of(2026, 6, 15, 0, 0)

            test("cutoff 이전에 soft delete 된 이미지만 반환한다 (active·cutoff 이후 제외)") {
                insertPostImage() // active → 제외
                val expiredId = insertPostImage(deleted = true, deletedDate = beforeCutoff)
                insertPostImage(deleted = true, deletedDate = afterCutoff) // cutoff 이후 → 제외

                val found = postImageQueryDao.findDeletedBefore(cutoff, null, 100)

                found shouldHaveSize 1
                found.first().id shouldBe expiredId
            }

            test("id ASC 순으로 정렬해 반환한다") {
                val firstId = insertPostImage(deleted = true, deletedDate = beforeCutoff)
                val secondId = insertPostImage(deleted = true, deletedDate = beforeCutoff)
                val thirdId = insertPostImage(deleted = true, deletedDate = beforeCutoff)

                val found = postImageQueryDao.findDeletedBefore(cutoff, null, 100)

                found.map { it.id } shouldBe listOf(firstId, secondId, thirdId)
            }

            test("cursorId 이후의 이미지만 반환한다") {
                val firstId = insertPostImage(deleted = true, deletedDate = beforeCutoff)
                val secondId = insertPostImage(deleted = true, deletedDate = beforeCutoff)
                val thirdId = insertPostImage(deleted = true, deletedDate = beforeCutoff)

                val found = postImageQueryDao.findDeletedBefore(cutoff, firstId, 100)

                found.map { it.id } shouldBe listOf(secondId, thirdId)
            }

            test("pageSize 만큼만 반환한다") {
                val firstId = insertPostImage(deleted = true, deletedDate = beforeCutoff)
                val secondId = insertPostImage(deleted = true, deletedDate = beforeCutoff)
                insertPostImage(deleted = true, deletedDate = beforeCutoff)

                val found = postImageQueryDao.findDeletedBefore(cutoff, null, 2)

                found.map { it.id } shouldBe listOf(firstId, secondId)
            }

            test("반환된 Entity 에 storageKey 와 thumbnailKey 가 매핑된다") {
                val storageKey = "community/post-image/9/keepsake.jpg"
                val thumbnailKey = "community/thumbnail/9/thumb_keepsake.jpg"
                insertPostImage(storageKey = storageKey, thumbnailKey = thumbnailKey, deleted = true, deletedDate = beforeCutoff)

                val found = postImageQueryDao.findDeletedBefore(cutoff, null, 100).first()

                found.storageKey shouldBe storageKey
                found.thumbnailKey shouldBe thumbnailKey
            }
        }

        context("delete") {

            test("해당 이미지 행을 물리 삭제한다") {
                val id = insertPostImage()

                postImageCommandDao.delete(id)

                PostImageTable.selectAll()
                    .where { PostImageTable.id eq id }
                    .count() shouldBe 0L
            }

            test("지정한 이미지만 삭제하고 다른 이미지는 유지한다") {
                val targetId = insertPostImage(postId = 1L)
                val survivorId = insertPostImage(postId = 2L)

                postImageCommandDao.delete(targetId)

                PostImageTable.selectAll().where { PostImageTable.id eq targetId }.count() shouldBe 0L
                PostImageTable.selectAll().where { PostImageTable.id eq survivorId }.count() shouldBe 1L
            }
        }
    }
}
