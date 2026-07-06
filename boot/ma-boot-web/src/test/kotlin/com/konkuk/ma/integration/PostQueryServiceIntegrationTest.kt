package com.konkuk.ma.integration

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.community.application.PostQueryService
import com.konkuk.ma.domain.community.domain.PostWithAuthor
import com.konkuk.ma.domain.community.entity.table.BlockTable
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostLikeTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.exception.EntityNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * 게시글 조회 상태 필드(REQ-011) Service 레이어 E2E 통합 테스트.
 *
 * PostQueryService.find(목록) / findDetail(상세) 가 실제 H2 DB 를 관통하여
 * likedByMe(내 활성 좋아요 여부) · isMine(작성자==조회자) · commentCount(soft-deleted 제외 루트+대댓글 수)
 * 를 정확히 조립하는지 검증한다.
 *
 * HTTP 응답 DTO 에 신규 필드가 아직 매핑되지 않은 단계(TDD RED)이므로 MockMvc 대신 Service 빈을 직접 주입해
 * 반환 read-model(PostWithAuthor/PostDetail) 을 단언한다. 그래도 Service→Domain→Repository→DAO→실제 DB 를
 * 관통하므로 쿼리 정합성(모킹 사각지대 없음)은 그대로 검증된다.
 */
@SpringBootTest
@ActiveProfiles("test")
class PostQueryServiceIntegrationTest(
    private val postQueryService: PostQueryService,
) : FunSpec({

    var memberSeq = 0L
    val defaultCursor = CursorIdCondition.of(null, null)

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, PostTable, CommentTable, PostLikeTable, CommentLikeTable, BlockTable)
        }
    }

    afterEach {
        transaction {
            BlockTable.deleteAll()
            PostLikeTable.deleteAll()
            CommentLikeTable.deleteAll()
            CommentTable.deleteAll()
            PostTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(BlockTable, PostLikeTable, CommentLikeTable, CommentTable, PostTable, MemberTable)
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

    fun insertPost(authorId: Long): Long {
        return transaction {
            PostTable.insertAndGetId {
                it[PostTable.authorId] = authorId
                it[category] = "CHEER"
                it[title] = "테스트 게시글"
                it[content] = "내용"
            }.value
        }
    }

    fun insertComment(
        postId: Long,
        authorId: Long,
        parentCommentId: Long? = null,
        deleted: Boolean = false,
    ): Long {
        return transaction {
            CommentTable.insertAndGetId {
                it[CommentTable.postId] = postId
                it[CommentTable.authorId] = authorId
                it[content] = "테스트 댓글"
                it[CommentTable.parentCommentId] = parentCommentId
                it[CommentTable.deleted] = deleted
            }.value
        }
    }

    fun likePost(postId: Long, memberId: Long, deleted: Boolean = false) {
        transaction {
            PostLikeTable.insert {
                it[PostLikeTable.postId] = postId
                it[PostLikeTable.memberId] = memberId
                it[PostLikeTable.deleted] = deleted
            }
        }
    }

    fun likeComment(commentId: Long, memberId: Long) {
        transaction {
            CommentLikeTable.insert {
                it[CommentLikeTable.commentId] = commentId
                it[CommentLikeTable.memberId] = memberId
            }
        }
    }

    fun blockMember(blockerId: Long, blockedId: Long, deleted: Boolean = false) {
        transaction {
            BlockTable.insert {
                it[BlockTable.blockerId] = blockerId
                it[BlockTable.blockedId] = blockedId
                it[BlockTable.deleted] = deleted
            }
        }
    }

    fun findPost(posts: List<PostWithAuthor>, postId: Long): PostWithAuthor {
        return posts.first { it.post.id == postId }
    }

    fun postIds(posts: List<PostWithAuthor>): List<Long> {
        return posts.map { it.post.id }
    }

    context("find - 게시글 목록 상태 필드") {

        test("내가 활성 좋아요한 게시글만 likedByMe=true 이고 타인 좋아요·취소한 좋아요는 false 다") {
            // Given - 조회자가 A만 좋아요, B는 타인이 좋아요, C는 조회자가 눌렀다 취소(soft delete)
            val viewerId = insertMember(nickname = "조회자")
            val authorId = insertMember(nickname = "작성자")
            val likedPost = insertPost(authorId)
            val othersLikedPost = insertPost(authorId)
            val cancelledPost = insertPost(authorId)
            likePost(likedPost, viewerId)
            likePost(othersLikedPost, authorId)
            likePost(cancelledPost, viewerId, deleted = true)

            // When
            val posts = postQueryService.find(null, defaultCursor, viewerId).data

            // Then
            findPost(posts, likedPost).likedByMe.shouldBeTrue()
            findPost(posts, othersLikedPost).likedByMe.shouldBeFalse()
            findPost(posts, cancelledPost).likedByMe.shouldBeFalse()
        }

        test("내가 작성한 게시글은 isMine=true, 타인 게시글은 isMine=false 다") {
            // Given
            val viewerId = insertMember(nickname = "조회자")
            val otherId = insertMember(nickname = "타인")
            val myPost = insertPost(viewerId)
            val othersPost = insertPost(otherId)

            // When
            val posts = postQueryService.find(null, defaultCursor, viewerId).data

            // Then
            findPost(posts, myPost).isMine.shouldBeTrue()
            findPost(posts, othersPost).isMine.shouldBeFalse()
        }

        test("commentCount는 soft-deleted를 제외한 루트+대댓글 수와 일치한다") {
            // Given - 활성 루트 2 + 활성 대댓글 1 + 삭제된 댓글 1
            val viewerId = insertMember()
            val authorId = insertMember()
            val postId = insertPost(authorId)
            val rootComment = insertComment(postId, authorId)
            insertComment(postId, authorId)
            insertComment(postId, authorId, parentCommentId = rootComment)
            insertComment(postId, authorId, deleted = true)

            // When
            val posts = postQueryService.find(null, defaultCursor, viewerId).data

            // Then - 삭제된 1건을 제외한 3건
            findPost(posts, postId).commentCount shouldBe 3
        }

        test("댓글이 하나도 없는 게시글의 commentCount는 0이다") {
            // Given
            val viewerId = insertMember()
            val authorId = insertMember()
            val postId = insertPost(authorId)

            // When
            val posts = postQueryService.find(null, defaultCursor, viewerId).data

            // Then
            findPost(posts, postId).commentCount shouldBe 0
        }

        test("게시글 30개를 한 번에 조회해도 각 게시글의 likedByMe·isMine·commentCount가 모두 정확하다 (N+1 없이 배치 조립 스케일 검증)") {
            // Given - 서로 다른 상태의 게시글 30개를 만들고 기대값을 함께 기록한다
            val viewerId = insertMember(nickname = "조회자")
            val otherId = insertMember(nickname = "타인")
            val expected = (0 until 30).associate { index ->
                val authorIsViewer = index % 2 == 0
                val postId = insertPost(if (authorIsViewer) viewerId else otherId)

                val viewerLikes = index % 3 == 0
                if (viewerLikes) likePost(postId, viewerId)
                likePost(postId, otherId) // 타인 좋아요는 likedByMe에 영향 없어야 한다

                val activeComments = index % 4
                repeat(activeComments) { insertComment(postId, otherId) }
                insertComment(postId, otherId, deleted = true) // 삭제 댓글은 commentCount에서 제외돼야 한다

                postId to Triple(authorIsViewer, viewerLikes, activeComments)
            }

            // When - 30개를 한 페이지로 조회
            val posts = postQueryService.find(null, CursorIdCondition.of(null, 50), viewerId).data

            // Then - 게시글 수와 상태 필드가 모두 기대값과 일치
            posts.size shouldBe 30
            expected.forEach { (postId, spec) ->
                val (authorIsViewer, viewerLikes, activeComments) = spec
                val post = findPost(posts, postId)
                post.isMine shouldBe authorIsViewer
                post.likedByMe shouldBe viewerLikes
                post.commentCount shouldBe activeComments
            }
        }
    }

    context("findDetail - 게시글 상세 상태 필드") {

        test("내가 작성하고 좋아요한 게시글 상세는 isMine=true, likedByMe=true 다") {
            // Given
            val viewerId = insertMember()
            val postId = insertPost(viewerId)
            likePost(postId, viewerId)

            // When
            val detail = postQueryService.findDetail(postId, viewerId)

            // Then
            detail.isMine.shouldBeTrue()
            detail.likedByMe.shouldBeTrue()
        }

        test("타인이 작성하고 내가 좋아요하지 않은 게시글 상세는 isMine=false, likedByMe=false 다") {
            // Given
            val viewerId = insertMember()
            val authorId = insertMember()
            val postId = insertPost(authorId)

            // When
            val detail = postQueryService.findDetail(postId, viewerId)

            // Then
            detail.isMine.shouldBeFalse()
            detail.likedByMe.shouldBeFalse()
        }

        test("상세의 각 댓글은 작성자/좋아요 여부에 따라 isMine·likedByMe가 채워진다") {
            // Given - 내가 쓴 댓글(내가 좋아요) + 타인 댓글
            val viewerId = insertMember()
            val authorId = insertMember()
            val postId = insertPost(authorId)
            val myComment = insertComment(postId, viewerId)
            val othersComment = insertComment(postId, authorId)
            likeComment(myComment, viewerId)

            // When
            val detail = postQueryService.findDetail(postId, viewerId)

            // Then
            val mine = detail.comments.first { it.comment.id == myComment }
            val others = detail.comments.first { it.comment.id == othersComment }
            mine.isMine.shouldBeTrue()
            mine.likedByMe.shouldBeTrue()
            others.isMine.shouldBeFalse()
            others.likedByMe.shouldBeFalse()
        }

        test("soft-deleted 댓글도 placeholder로 노출되며 isMine은 실제 작성자 기준으로 유지된다") {
            // Given - 내가 쓴 뒤 삭제된 댓글
            val viewerId = insertMember()
            val postId = insertPost(viewerId)
            val deletedComment = insertComment(postId, viewerId, deleted = true)

            // When
            val detail = postQueryService.findDetail(postId, viewerId)

            // Then - 내용은 placeholder이지만 isMine은 유지된다
            val placeholder = detail.comments.first { it.comment.id == deletedComment }
            placeholder.comment.displayContent() shouldBe "삭제된 댓글입니다."
            placeholder.isMine.shouldBeTrue()
        }
    }

    context("find - 차단 필터") {

        test("차단한 작성자의 게시글은 목록에서 제외되고 다른 작성자의 게시글만 노출된다") {
            // Given - 조회자가 blockedAuthor를 차단, 각 작성자가 게시글 1개씩 작성
            val viewerId = insertMember(nickname = "조회자")
            val blockedAuthorId = insertMember(nickname = "차단대상")
            val visibleAuthorId = insertMember(nickname = "비차단")
            val blockedPost = insertPost(blockedAuthorId)
            val visiblePost = insertPost(visibleAuthorId)
            blockMember(blockerId = viewerId, blockedId = blockedAuthorId)

            // When
            val posts = postQueryService.find(null, defaultCursor, viewerId).data

            // Then
            postIds(posts) shouldNotContain blockedPost
            postIds(posts) shouldContain visiblePost
        }

        test("차단 작성자 게시글을 제외해도 나머지 게시글 수와 정렬(id 내림차순)이 유지된다") {
            // Given - 비차단 작성자 게시글 3개 사이에 차단 작성자 게시글 1개가 섞여 있다
            val viewerId = insertMember(nickname = "조회자")
            val blockedAuthorId = insertMember(nickname = "차단대상")
            val visibleAuthorId = insertMember(nickname = "비차단")
            val first = insertPost(visibleAuthorId)
            insertPost(blockedAuthorId)
            val second = insertPost(visibleAuthorId)
            val third = insertPost(visibleAuthorId)
            blockMember(blockerId = viewerId, blockedId = blockedAuthorId)

            // When
            val posts = postQueryService.find(null, defaultCursor, viewerId).data

            // Then - 차단 게시글만 빠지고 나머지 3개가 id 내림차순으로 유지된다
            postIds(posts) shouldBe listOf(third, second, first)
        }

        test("해제(soft delete)된 차단은 목록 필터에 영향을 주지 않아 해당 작성자 게시글이 노출된다") {
            // Given - 차단이 이미 해제된 상태
            val viewerId = insertMember(nickname = "조회자")
            val unblockedAuthorId = insertMember(nickname = "해제된차단대상")
            val visiblePost = insertPost(unblockedAuthorId)
            blockMember(blockerId = viewerId, blockedId = unblockedAuthorId, deleted = true)

            // When
            val posts = postQueryService.find(null, defaultCursor, viewerId).data

            // Then
            postIds(posts) shouldContain visiblePost
        }
    }

    context("findDetail - 차단 필터") {

        test("차단한 작성자의 게시글을 상세 조회하면 EntityNotFoundException이 발생한다") {
            // Given - 조회자가 게시글 작성자를 차단
            val viewerId = insertMember(nickname = "조회자")
            val blockedAuthorId = insertMember(nickname = "차단대상")
            val postId = insertPost(blockedAuthorId)
            blockMember(blockerId = viewerId, blockedId = blockedAuthorId)

            // When & Then
            shouldThrow<EntityNotFoundException> {
                postQueryService.findDetail(postId, viewerId)
            }
        }

        test("차단한 작성자의 댓글은 placeholder로 노출되고 blockedAuthor=true다") {
            // Given - 게시글 작성자는 차단하지 않고(상세 조회 가능) 댓글 작성자만 차단
            val viewerId = insertMember(nickname = "조회자")
            val postAuthorId = insertMember(nickname = "게시글작성자")
            val blockedCommentAuthorId = insertMember(nickname = "차단댓글작성자")
            val postId = insertPost(postAuthorId)
            val blockedComment = insertComment(postId, blockedCommentAuthorId)
            blockMember(blockerId = viewerId, blockedId = blockedCommentAuthorId)

            // When
            val detail = postQueryService.findDetail(postId, viewerId)

            // Then
            val comment = detail.comments.first { it.comment.id == blockedComment }
            comment.blockedAuthor.shouldBeTrue()
            comment.displayContent() shouldBe "차단한 사용자의 댓글입니다."
        }

        test("차단하지 않은 작성자의 댓글은 원문과 blockedAuthor=false로 노출된다") {
            // Given - 아무도 차단하지 않음
            val viewerId = insertMember(nickname = "조회자")
            val postAuthorId = insertMember(nickname = "게시글작성자")
            val commentAuthorId = insertMember(nickname = "댓글작성자")
            val postId = insertPost(postAuthorId)
            val comment = insertComment(postId, commentAuthorId)

            // When
            val detail = postQueryService.findDetail(postId, viewerId)

            // Then
            val target = detail.comments.first { it.comment.id == comment }
            target.blockedAuthor.shouldBeFalse()
            target.displayContent() shouldBe "테스트 댓글"
        }
    }
})
