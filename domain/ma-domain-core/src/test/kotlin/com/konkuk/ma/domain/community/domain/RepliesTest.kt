package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.fixture.CommentFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.Members
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class RepliesTest : FunSpec({

    context("previewFor") {

        test("부모 댓글에 대한 CommentWithPreviewReplies를 반환한다") {
            // Given
            val parent = CommentFixture.create(id = 1L)
            val reply1 = CommentFixture.create(id = 2L, parentCommentId = parent.id)
            val reply2 = CommentFixture.create(id = 3L, parentCommentId = parent.id)
            val replies = Replies(listOf(reply1, reply2))

            // When
            val result = replies.previewFor(parent)

            // Then
            result.parent shouldBe parent
            result.previewReplies.data shouldHaveSize 2
            result.remainingReplyCount shouldBe 0
        }

        test("대댓글이 3개를 초과하면 최신순 3개만 미리보기로 포함한다") {
            // Given
            val parent = CommentFixture.create(id = 1L)
            val now = LocalDateTime.of(2025, 1, 1, 12, 0)
            val allReplies = (1..5).map { i ->
                CommentFixture.create(
                    id = (i + 1).toLong(),
                    parentCommentId = parent.id,
                    createdDate = now.plusMinutes(i.toLong()),
                )
            }
            val replies = Replies(allReplies)

            // When
            val result = replies.previewFor(parent)

            // Then
            result.previewReplies.data shouldHaveSize 3
            result.remainingReplyCount shouldBe 2
            // 최신순 정렬 확인: 5분 > 4분 > 3분
            result.previewReplies.data[0].createdDate shouldBe now.plusMinutes(5)
            result.previewReplies.data[1].createdDate shouldBe now.plusMinutes(4)
            result.previewReplies.data[2].createdDate shouldBe now.plusMinutes(3)
        }

        test("대댓글이 정확히 3개이면 remainingReplyCount는 0이다") {
            // Given
            val parent = CommentFixture.create(id = 1L)
            val allReplies = (1..3).map { i ->
                CommentFixture.create(id = (i + 1).toLong(), parentCommentId = parent.id)
            }
            val replies = Replies(allReplies)

            // When
            val result = replies.previewFor(parent)

            // Then
            result.previewReplies.data shouldHaveSize 3
            result.remainingReplyCount shouldBe 0
        }

        test("대댓글이 없으면 빈 미리보기와 remainingReplyCount 0을 반환한다") {
            // Given
            val parent = CommentFixture.create(id = 1L)
            val replies = Replies(emptyList())

            // When
            val result = replies.previewFor(parent)

            // Then
            result.previewReplies.data shouldHaveSize 0
            result.remainingReplyCount shouldBe 0
        }
    }

    context("combineWithAuthors") {

        test("대댓글에 작성자 닉네임이 매핑된다") {
            // Given
            val reply1 = CommentFixture.create(id = 2L, authorEmail = "reply1@example.com", parentCommentId = 1L)
            val reply2 = CommentFixture.create(id = 3L, authorEmail = "reply2@example.com", parentCommentId = 1L)
            val replies = Replies(listOf(reply1, reply2))
            val members = Members(
                listOf(
                    MemberFixture.create(email = reply1.authorEmail.value, nickname = "대댓글작성자1"),
                    MemberFixture.create(email = reply2.authorEmail.value, nickname = "대댓글작성자2"),
                )
            )

            // When
            val result = replies.combineWithAuthors(members, LikeCounts.from(emptyMap()))

            // Then
            result shouldHaveSize 2
            result[0].comment shouldBe reply1
            result[0].nickname shouldBe "대댓글작성자1"
            result[1].comment shouldBe reply2
            result[1].nickname shouldBe "대댓글작성자2"
        }

        test("탈퇴한 회원의 닉네임은 '알 수 없음'으로 표시된다") {
            // Given
            val reply = CommentFixture.create(id = 2L, authorEmail = "deleted@example.com", parentCommentId = 1L)
            val replies = Replies(listOf(reply))
            val members = Members(emptyList())

            // When
            val result = replies.combineWithAuthors(members, LikeCounts.from(emptyMap()))

            // Then
            result shouldHaveSize 1
            result[0].nickname shouldBe "알 수 없음"
        }

        test("대댓글이 없으면 빈 목록을 반환한다") {
            // Given
            val replies = Replies(emptyList())
            val members = Members(emptyList())

            // When
            val result = replies.combineWithAuthors(members, LikeCounts.from(emptyMap()))

            // Then
            result shouldHaveSize 0
        }
    }
})
