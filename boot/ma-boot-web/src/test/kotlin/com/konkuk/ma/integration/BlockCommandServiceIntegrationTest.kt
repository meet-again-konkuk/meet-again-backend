package com.konkuk.ma.integration

import com.konkuk.ma.domain.community.application.BlockCommandService
import com.konkuk.ma.domain.community.entity.table.BlockTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * 차단 등록/해제(REQ-014) Service 레이어 E2E 통합 테스트.
 *
 * BlockCommandService.blockPostAuthor/blockCommentAuthor/unblock 이 실제 H2 DB 를 관통하여
 * 최초 차단(newlyBlocked=true)·재차단 idempotent(같은 blockId, false)·본인 차단(400)·
 * 소유권 없는 해제(403)·없는 차단 해제(404)·정상 soft delete 를 정확히 처리하는지 검증한다.
 *
 * Service 본문이 아직 TODO 인 TDD RED 단계이므로 MockMvc 대신 Service 빈을 직접 주입한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class BlockCommandServiceIntegrationTest(
    private val blockCommandService: BlockCommandService,
) : FunSpec({

    var memberSeq = 0L

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, PostTable, CommentTable, BlockTable)
        }
    }

    afterEach {
        transaction {
            BlockTable.deleteAll()
            CommentTable.deleteAll()
            PostTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(BlockTable, CommentTable, PostTable, MemberTable)
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
                it[title] = "게시글"
                it[content] = "내용"
            }.value
        }
    }

    fun insertComment(postId: Long, authorId: Long): Long {
        return transaction {
            CommentTable.insertAndGetId {
                it[CommentTable.postId] = postId
                it[CommentTable.authorId] = authorId
                it[content] = "댓글"
            }.value
        }
    }

    fun insertBlock(blockerId: Long, blockedId: Long): Long {
        return transaction {
            BlockTable.insertAndGetId {
                it[BlockTable.blockerId] = blockerId
                it[BlockTable.blockedId] = blockedId
            }.value
        }
    }

    fun countActiveBlocks(blockerId: Long, blockedId: Long): Long {
        return transaction {
            BlockTable.selectAll()
                .where {
                    (BlockTable.blockerId eq blockerId) and
                        (BlockTable.blockedId eq blockedId) and
                        (BlockTable.deleted eq false)
                }
                .count()
        }
    }

    fun isBlockDeleted(blockId: Long): Boolean {
        return transaction {
            BlockTable.selectAll()
                .where { BlockTable.id eq blockId }
                .single()[BlockTable.deleted]
        }
    }

    context("blockPostAuthor") {

        test("게시글 작성자를 처음 차단하면 newlyBlocked=true이고 차단당한 회원 닉네임을 반환한다") {
            // Given
            val blockerId = insertMember(nickname = "차단자")
            val author = insertMember(nickname = "차단대상")
            val postId = insertPost(authorId = author)

            // When
            val result = blockCommandService.blockPostAuthor(postId, blockerId)

            // Then
            result.blockId.shouldBeGreaterThan(0L)
            result.newlyBlocked.shouldBeTrue()
            result.blockedNickname shouldBe "차단대상"
            countActiveBlocks(blockerId, author) shouldBe 1L
        }

        test("이미 차단한 작성자를 다시 차단하면 newlyBlocked=false이고 기존 blockId를 그대로 반환한다") {
            // Given
            val blockerId = insertMember(nickname = "차단자")
            val author = insertMember(nickname = "차단대상")
            val postId = insertPost(authorId = author)
            val first = blockCommandService.blockPostAuthor(postId, blockerId)

            // When
            val second = blockCommandService.blockPostAuthor(postId, blockerId)

            // Then - 멱등: 같은 blockId, 활성 행은 여전히 1개
            second.newlyBlocked.shouldBeFalse()
            second.blockId shouldBe first.blockId
            countActiveBlocks(blockerId, author) shouldBe 1L
        }

        test("본인이 작성한 게시글의 작성자(=본인)를 차단하면 InvalidValueException이 발생한다") {
            // Given
            val author = insertMember(nickname = "작성자")
            val postId = insertPost(authorId = author)

            // When & Then
            shouldThrow<InvalidValueException> {
                blockCommandService.blockPostAuthor(postId, author)
            }
        }

        test("존재하지 않는 게시글의 작성자를 차단하면 EntityNotFoundException이 발생한다") {
            // Given
            val blockerId = insertMember(nickname = "차단자")
            val notExistingPostId = 999_999L

            // When & Then
            shouldThrow<EntityNotFoundException> {
                blockCommandService.blockPostAuthor(notExistingPostId, blockerId)
            }
        }
    }

    context("blockCommentAuthor") {

        test("댓글 작성자를 처음 차단하면 newlyBlocked=true이고 차단당한 회원 닉네임을 반환한다") {
            // Given
            val blockerId = insertMember(nickname = "차단자")
            val author = insertMember(nickname = "댓글작성자")
            val postId = insertPost(authorId = blockerId)
            val commentId = insertComment(postId = postId, authorId = author)

            // When
            val result = blockCommandService.blockCommentAuthor(commentId, blockerId)

            // Then
            result.newlyBlocked.shouldBeTrue()
            result.blockedNickname shouldBe "댓글작성자"
            countActiveBlocks(blockerId, author) shouldBe 1L
        }

        test("본인이 작성한 댓글의 작성자(=본인)를 차단하면 InvalidValueException이 발생한다") {
            // Given
            val author = insertMember(nickname = "작성자")
            val postId = insertPost(authorId = author)
            val commentId = insertComment(postId = postId, authorId = author)

            // When & Then
            shouldThrow<InvalidValueException> {
                blockCommandService.blockCommentAuthor(commentId, author)
            }
        }

        test("존재하지 않는 댓글의 작성자를 차단하면 EntityNotFoundException이 발생한다") {
            // Given
            val blockerId = insertMember(nickname = "차단자")
            val notExistingCommentId = 999_999L

            // When & Then
            shouldThrow<EntityNotFoundException> {
                blockCommandService.blockCommentAuthor(notExistingCommentId, blockerId)
            }
        }
    }

    context("unblock") {

        test("본인이 만든 차단을 해제하면 해당 차단 행이 soft delete 된다") {
            // Given
            val blockerId = insertMember(nickname = "차단자")
            val blockedId = insertMember(nickname = "차단대상")
            val blockId = insertBlock(blockerId = blockerId, blockedId = blockedId)

            // When
            blockCommandService.unblock(blockId, blockerId)

            // Then
            isBlockDeleted(blockId).shouldBeTrue()
            countActiveBlocks(blockerId, blockedId) shouldBe 0L
        }

        test("타인이 만든 차단을 해제하려 하면 AccessDeniedException이 발생한다") {
            // Given
            val ownerId = insertMember(nickname = "차단소유자")
            val blockedId = insertMember(nickname = "차단대상")
            val otherId = insertMember(nickname = "다른회원")
            val blockId = insertBlock(blockerId = ownerId, blockedId = blockedId)

            // When & Then
            shouldThrow<AccessDeniedException> {
                blockCommandService.unblock(blockId, otherId)
            }
        }

        test("존재하지 않는 차단을 해제하려 하면 EntityNotFoundException이 발생한다") {
            // Given
            val blockerId = insertMember(nickname = "차단자")
            val notExistingBlockId = 999_999L

            // When & Then
            shouldThrow<EntityNotFoundException> {
                blockCommandService.unblock(notExistingBlockId, blockerId)
            }
        }
    }
})
