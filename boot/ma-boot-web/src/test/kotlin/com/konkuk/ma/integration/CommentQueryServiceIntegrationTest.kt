package com.konkuk.ma.integration

import com.konkuk.ma.domain.community.application.CommentQueryService
import com.konkuk.ma.domain.community.entity.table.BlockTable
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
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
 * 루트 댓글 상세 조회 상태 필드(REQ-011) Service 레이어 E2E 통합 테스트.
 *
 * CommentQueryService.findDetail 이 실제 H2 DB 를 관통하여 루트 댓글과 각 대댓글의
 * likedByMe(내 활성 좋아요 여부) · isMine(작성자==조회자) 를 정확히 조립하는지 검증한다.
 *
 * HTTP 응답 DTO 에 신규 필드가 아직 매핑되지 않은 단계(TDD RED)이므로 MockMvc 대신 Service 빈을 직접 주입해
 * 반환 read-model(CommentWithAuthor) 을 단언한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class CommentQueryServiceIntegrationTest(
    private val commentQueryService: CommentQueryService,
) : FunSpec({

    var memberSeq = 0L

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, PostTable, CommentTable, CommentLikeTable, BlockTable)
        }
    }

    afterEach {
        transaction {
            BlockTable.deleteAll()
            CommentLikeTable.deleteAll()
            CommentTable.deleteAll()
            PostTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(BlockTable, CommentLikeTable, CommentTable, PostTable, MemberTable)
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
    ): Long {
        return transaction {
            CommentTable.insertAndGetId {
                it[CommentTable.postId] = postId
                it[CommentTable.authorId] = authorId
                it[content] = "테스트 댓글"
                it[CommentTable.parentCommentId] = parentCommentId
            }.value
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

    fun blockMember(blockerId: Long, blockedId: Long) {
        transaction {
            BlockTable.insert {
                it[BlockTable.blockerId] = blockerId
                it[BlockTable.blockedId] = blockedId
            }
        }
    }

    context("findDetail - 루트 댓글 상세 상태 필드") {

        test("내가 작성하고 좋아요한 루트 댓글은 isMine=true, likedByMe=true 다") {
            // Given
            val viewerId = insertMember()
            val postId = insertPost(viewerId)
            val rootCommentId = insertComment(postId, viewerId)
            likeComment(rootCommentId, viewerId)

            // When
            val root = commentQueryService.findDetail(rootCommentId, viewerId)

            // Then
            root.comment.id shouldBe rootCommentId
            root.isMine.shouldBeTrue()
            root.likedByMe.shouldBeTrue()
        }

        test("타인이 작성하고 내가 좋아요하지 않은 루트 댓글은 isMine=false, likedByMe=false 다") {
            // Given
            val viewerId = insertMember()
            val authorId = insertMember()
            val postId = insertPost(authorId)
            val rootCommentId = insertComment(postId, authorId)

            // When
            val root = commentQueryService.findDetail(rootCommentId, viewerId)

            // Then
            root.isMine.shouldBeFalse()
            root.likedByMe.shouldBeFalse()
        }

        test("대댓글도 작성자/좋아요 여부에 따라 isMine·likedByMe가 채워진다") {
            // Given - 내가 쓴 대댓글(내가 좋아요) + 타인 대댓글
            val viewerId = insertMember()
            val authorId = insertMember()
            val postId = insertPost(authorId)
            val rootCommentId = insertComment(postId, authorId)
            val myReply = insertComment(postId, viewerId, parentCommentId = rootCommentId)
            val othersReply = insertComment(postId, authorId, parentCommentId = rootCommentId)
            likeComment(myReply, viewerId)

            // When
            val root = commentQueryService.findDetail(rootCommentId, viewerId)

            // Then
            val mine = root.replies.first { it.comment.id == myReply }
            val others = root.replies.first { it.comment.id == othersReply }
            mine.isMine.shouldBeTrue()
            mine.likedByMe.shouldBeTrue()
            others.isMine.shouldBeFalse()
            others.likedByMe.shouldBeFalse()
        }
    }

    context("findDetail - 차단 필터") {

        test("차단한 작성자의 루트 댓글은 placeholder로 노출되고 blockedAuthor=true다") {
            // Given - 조회자가 루트 댓글 작성자를 차단
            val viewerId = insertMember(nickname = "조회자")
            val blockedAuthorId = insertMember(nickname = "차단대상")
            val postId = insertPost(blockedAuthorId)
            val rootCommentId = insertComment(postId, blockedAuthorId)
            blockMember(blockerId = viewerId, blockedId = blockedAuthorId)

            // When
            val root = commentQueryService.findDetail(rootCommentId, viewerId)

            // Then
            root.blockedAuthor.shouldBeTrue()
            root.displayContent() shouldBe "차단한 사용자의 댓글입니다."
        }

        test("차단한 작성자의 대댓글은 placeholder로 노출되고 blockedAuthor=true다") {
            // Given - 루트 댓글 작성자는 차단하지 않고 대댓글 작성자만 차단
            val viewerId = insertMember(nickname = "조회자")
            val rootAuthorId = insertMember(nickname = "루트작성자")
            val blockedReplyAuthorId = insertMember(nickname = "차단대댓글작성자")
            val postId = insertPost(rootAuthorId)
            val rootCommentId = insertComment(postId, rootAuthorId)
            val blockedReply = insertComment(postId, blockedReplyAuthorId, parentCommentId = rootCommentId)
            blockMember(blockerId = viewerId, blockedId = blockedReplyAuthorId)

            // When
            val root = commentQueryService.findDetail(rootCommentId, viewerId)

            // Then - 루트는 정상, 대댓글만 placeholder
            root.blockedAuthor.shouldBeFalse()
            val reply = root.replies.first { it.comment.id == blockedReply }
            reply.blockedAuthor.shouldBeTrue()
            reply.displayContent() shouldBe "차단한 사용자의 댓글입니다."
        }

        test("차단하지 않은 작성자의 대댓글은 원문과 blockedAuthor=false로 노출된다") {
            // Given - 아무도 차단하지 않음
            val viewerId = insertMember(nickname = "조회자")
            val rootAuthorId = insertMember(nickname = "루트작성자")
            val replyAuthorId = insertMember(nickname = "대댓글작성자")
            val postId = insertPost(rootAuthorId)
            val rootCommentId = insertComment(postId, rootAuthorId)
            val reply = insertComment(postId, replyAuthorId, parentCommentId = rootCommentId)

            // When
            val root = commentQueryService.findDetail(rootCommentId, viewerId)

            // Then
            val target = root.replies.first { it.comment.id == reply }
            target.blockedAuthor.shouldBeFalse()
            target.displayContent() shouldBe "테스트 댓글"
        }
    }
})
