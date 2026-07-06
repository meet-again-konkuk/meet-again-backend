package com.konkuk.ma.integration

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.community.application.PostCommandService
import com.konkuk.ma.domain.community.application.PostQueryService
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.PostDetails
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostLikeTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * 게시글 수정/삭제(REQ-012) Service 레이어 E2E 통합 테스트.
 *
 * PostCommandService.update/delete 가 실제 H2 DB 를 관통하여
 * 소유권 검증 · 최신 값 갱신 · 좋아요/댓글 보존 · soft delete · 반복 삭제 404 를 정확히 처리하는지 검증한다.
 *
 * (kotest-writing 규칙: Service 는 모킹 단위 테스트가 아니라 실제 DB 를 관통하는 E2E 로 검증한다.
 *  HTTP 계층은 별도 MockMvc E2E(PostCommandIntegrationTest)에서 검증하므로 여기서는 Service 빈을 직접 주입한다.)
 */
@SpringBootTest
@ActiveProfiles("test")
class PostCommandServiceIntegrationTest(
    private val postCommandService: PostCommandService,
    private val postQueryService: PostQueryService,
) : FunSpec({

    var memberSeq = 0L
    val defaultCursor = CursorIdCondition.of(null, null)

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, PostTable, CommentTable, PostLikeTable, CommentLikeTable)
        }
    }

    afterEach {
        transaction {
            PostLikeTable.deleteAll()
            CommentLikeTable.deleteAll()
            CommentTable.deleteAll()
            PostTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(PostLikeTable, CommentLikeTable, CommentTable, PostTable, MemberTable)
        }
    }

    fun insertMember(nickname: String = "회원"): Long {
        memberSeq += 1
        return transaction {
            MemberTable.insertAndGetId {
                it[MemberTable.email] = "member$memberSeq@example.com"
                it[password] = "encoded"
                it[MemberTable.nickname] = nickname
                it[gender] = "MALE"
                it[phoneNumber] = "01012345678"
                it[name] = "김테스트"
                it[birthDate] = LocalDate.of(1990, 1, 1)
                it[region] = "SEOUL"
            }.value
        }
    }

    fun insertPost(
        authorId: Long,
        category: PostCategory = PostCategory.SUCCESS_STORY,
        title: String = "원본 제목",
        content: String = "원본 내용",
        createdDate: LocalDateTime? = null,
        deleted: Boolean = false,
    ): Long {
        return transaction {
            PostTable.insertAndGetId {
                it[PostTable.authorId] = authorId
                it[PostTable.category] = category.name
                it[PostTable.title] = title
                it[PostTable.content] = content
                it[PostTable.deleted] = deleted
                if (createdDate != null) it[PostTable.createdDate] = createdDate
            }.value
        }
    }

    fun insertComment(postId: Long, authorId: Long, parentCommentId: Long? = null): Long {
        return transaction {
            CommentTable.insertAndGetId {
                it[CommentTable.postId] = postId
                it[CommentTable.authorId] = authorId
                it[content] = "테스트 댓글"
                it[CommentTable.parentCommentId] = parentCommentId
            }.value
        }
    }

    fun likePost(postId: Long, memberId: Long) {
        transaction {
            PostLikeTable.insert {
                it[PostLikeTable.postId] = postId
                it[PostLikeTable.memberId] = memberId
            }
        }
    }

    fun countActiveComments(postId: Long): Long {
        return transaction {
            CommentTable.selectAll()
                .where { (CommentTable.postId eq postId) and (CommentTable.deleted eq false) }
                .count()
        }
    }

    fun countActiveLikes(postId: Long): Long {
        return transaction {
            PostLikeTable.selectAll()
                .where { (PostLikeTable.postId eq postId) and (PostLikeTable.deleted eq false) }
                .count()
        }
    }

    fun readPostRow(postId: Long) = transaction {
        PostTable.selectAll().where { PostTable.id eq postId }.first()
    }

    context("update") {

        test("본인이 수정하면 카테고리·제목·내용이 최신 값으로 갱신된다") {
            // Given
            val authorId = insertMember()
            val postId = insertPost(authorId, category = PostCategory.SUCCESS_STORY, title = "원본 제목", content = "원본 내용")
            val details = PostDetails(PostCategory.COUNSELING, "수정된 제목", "수정된 내용")

            // When
            postCommandService.update(postId, authorId, details)

            // Then
            val detail = postQueryService.findDetail(postId, authorId)
            detail.post.category shouldBe PostCategory.COUNSELING
            detail.post.title shouldBe "수정된 제목"
            detail.post.content shouldBe "수정된 내용"
        }

        test("본인이 수정해도 좋아요와 댓글(대댓글 포함)은 보존된다") {
            // Given - 루트 댓글 1 + 대댓글 1 + 좋아요 2개
            val authorId = insertMember()
            val likerId = insertMember()
            val postId = insertPost(authorId)
            val rootCommentId = insertComment(postId, authorId)
            insertComment(postId, likerId, parentCommentId = rootCommentId)
            likePost(postId, authorId)
            likePost(postId, likerId)
            val details = PostDetails(PostCategory.CHEER, "수정된 제목", "수정된 내용")

            // When
            postCommandService.update(postId, authorId, details)

            // Then - 수정 후에도 댓글 2건·좋아요 2건이 그대로 유지된다
            countActiveComments(postId) shouldBe 2L
            countActiveLikes(postId) shouldBe 2L
            postQueryService.findDetail(postId, authorId).likeCount shouldBe 2
        }

        test("본인이 수정해도 createdDate는 유지되고 lastModifiedDate는 갱신된다") {
            // Given - createdDate 를 과거 시각으로 고정해 둔다
            val authorId = insertMember()
            val originalCreatedDate = LocalDateTime.of(2020, 1, 1, 0, 0, 0)
            val postId = insertPost(authorId, createdDate = originalCreatedDate)
            val details = PostDetails(PostCategory.CHEER, "수정된 제목", "수정된 내용")

            // When
            postCommandService.update(postId, authorId, details)

            // Then - createdDate 는 그대로, lastModifiedDate 는 createdDate 이후로 갱신된다
            val row = readPostRow(postId)
            row[PostTable.createdDate] shouldBe originalCreatedDate
            row[PostTable.lastModifiedDate].isAfter(originalCreatedDate).shouldBeTrue()
        }

        test("타인이 수정하면 AccessDeniedException이 발생한다") {
            // Given
            val authorId = insertMember()
            val otherId = insertMember()
            val postId = insertPost(authorId)
            val details = PostDetails(PostCategory.CHEER, "수정된 제목", "수정된 내용")

            // When & Then
            shouldThrow<AccessDeniedException> {
                postCommandService.update(postId, otherId, details)
            }
        }

        test("존재하지 않는 게시글을 수정하면 EntityNotFoundException이 발생한다") {
            // Given
            val memberId = insertMember()
            val nonExistentPostId = 999L
            val details = PostDetails(PostCategory.CHEER, "수정된 제목", "수정된 내용")

            // When & Then
            shouldThrow<EntityNotFoundException> {
                postCommandService.update(nonExistentPostId, memberId, details)
            }
        }

        test("이미 삭제(soft delete)된 게시글을 수정하면 EntityNotFoundException이 발생한다") {
            // Given
            val authorId = insertMember()
            val postId = insertPost(authorId, deleted = true)
            val details = PostDetails(PostCategory.CHEER, "수정된 제목", "수정된 내용")

            // When & Then
            shouldThrow<EntityNotFoundException> {
                postCommandService.update(postId, authorId, details)
            }
        }
    }

    context("delete") {

        test("본인이 삭제하면 게시글 행이 soft delete(deleted=true) 처리된다") {
            // Given
            val authorId = insertMember()
            val postId = insertPost(authorId)

            // When
            postCommandService.delete(postId, authorId)

            // Then
            readPostRow(postId)[PostTable.deleted].shouldBeTrue()
        }

        test("본인이 삭제하면 상세 조회 시 EntityNotFoundException이 발생한다") {
            // Given
            val authorId = insertMember()
            val postId = insertPost(authorId)

            // When
            postCommandService.delete(postId, authorId)

            // Then
            shouldThrow<EntityNotFoundException> {
                postQueryService.findDetail(postId, authorId)
            }
        }

        test("본인이 삭제하면 목록 조회에서 제외된다") {
            // Given
            val authorId = insertMember()
            val postId = insertPost(authorId)

            // When
            postCommandService.delete(postId, authorId)

            // Then
            val postIds = postQueryService.find(null, defaultCursor, authorId).data.map { it.post.id }
            postIds shouldNotContain postId
        }

        test("타인이 삭제하면 AccessDeniedException이 발생한다") {
            // Given
            val authorId = insertMember()
            val otherId = insertMember()
            val postId = insertPost(authorId)

            // When & Then
            shouldThrow<AccessDeniedException> {
                postCommandService.delete(postId, otherId)
            }
        }

        test("존재하지 않는 게시글을 삭제하면 EntityNotFoundException이 발생한다") {
            // Given
            val memberId = insertMember()
            val nonExistentPostId = 999L

            // When & Then
            shouldThrow<EntityNotFoundException> {
                postCommandService.delete(nonExistentPostId, memberId)
            }
        }

        test("이미 삭제된 게시글을 다시 삭제하면 EntityNotFoundException이 발생한다") {
            // Given - 한 번 삭제한 상태
            val authorId = insertMember()
            val postId = insertPost(authorId)
            postCommandService.delete(postId, authorId)

            // When & Then - 반복 삭제는 404
            shouldThrow<EntityNotFoundException> {
                postCommandService.delete(postId, authorId)
            }
        }
    }
})
