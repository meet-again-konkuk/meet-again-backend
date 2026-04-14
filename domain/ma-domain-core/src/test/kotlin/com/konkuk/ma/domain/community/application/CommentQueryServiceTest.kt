package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.exception.InvalidStateException
import com.konkuk.ma.domain.community.fixture.CommentFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class CommentQueryServiceTest : FunSpec({

    val commentQueryRepository = mockk<CommentQueryRepository>()
    val memberQueryRepository = mockk<MemberQueryRepository>()
    val service = CommentQueryService(commentQueryRepository, memberQueryRepository)

    beforeEach {
        clearAllMocks()
    }

    context("findDetail") {

        test("루트 댓글과 대댓글을 조회하고 작성자 닉네임을 매핑하여 반환한다") {
            // Given
            val rootComment = CommentFixture.create(authorEmail = "root@example.com")
            val reply = CommentFixture.create(id = 2L, authorEmail = "reply@example.com", parentCommentId = rootComment.id)
            val rootMember = MemberFixture.create(email = rootComment.authorEmail.value, nickname = "루트작성자")
            val replyMember = MemberFixture.create(email = reply.authorEmail.value, nickname = "대댓글작성자")

            every { commentQueryRepository.findOne(rootComment.id) } returns rootComment
            every { commentQueryRepository.findReplies(rootComment.id) } returns listOf(reply)
            every {
                memberQueryRepository.findByEmails(setOf(reply.authorEmail, rootComment.authorEmail))
            } returns listOf(rootMember, replyMember)

            // When
            val result = service.findDetail(rootComment.id)

            // Then
            result.comment shouldBe rootComment
            result.nickname shouldBe rootMember.nickname
            result.replies shouldHaveSize 1
            result.replies[0].nickname shouldBe replyMember.nickname
            result.remainingReplyCount shouldBe 0
        }

        test("대댓글이 없으면 빈 대댓글 목록을 반환한다") {
            // Given
            val rootComment = CommentFixture.create(authorEmail = "root@example.com")
            val rootMember = MemberFixture.create(email = rootComment.authorEmail.value, nickname = "루트작성자")

            every { commentQueryRepository.findOne(rootComment.id) } returns rootComment
            every { commentQueryRepository.findReplies(rootComment.id) } returns emptyList()
            every {
                memberQueryRepository.findByEmails(setOf(rootComment.authorEmail))
            } returns listOf(rootMember)

            // When
            val result = service.findDetail(rootComment.id)

            // Then
            result.comment shouldBe rootComment
            result.nickname shouldBe rootMember.nickname
            result.replies shouldHaveSize 0
            result.remainingReplyCount shouldBe 0
        }

        test("존재하지 않는 댓글을 조회하면 EntityNotFoundException이 발생한다") {
            // Given
            val nonExistentId = 999L

            every { commentQueryRepository.findOne(nonExistentId) } throws EntityNotFoundException(
                EntityType.COMMUNITY_COMMENT, nonExistentId.toString()
            )

            // When & Then
            shouldThrow<EntityNotFoundException> {
                service.findDetail(nonExistentId)
            }.message shouldBe "CommunityComment을(를) 찾을 수 없습니다."
        }

        test("대댓글 ID로 조회하면 InvalidStateException이 발생한다") {
            // Given
            val replyComment = CommentFixture.create(id = 5L, parentCommentId = 1L)

            every { commentQueryRepository.findOne(replyComment.id) } returns replyComment

            // When & Then
            shouldThrow<InvalidStateException> {
                service.findDetail(replyComment.id)
            }
        }
    }
})
