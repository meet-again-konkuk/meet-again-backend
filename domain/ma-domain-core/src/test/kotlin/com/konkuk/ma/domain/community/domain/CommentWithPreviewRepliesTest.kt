package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.fixture.CommentFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.Members
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class CommentWithPreviewRepliesTest : FunSpec({

    context("combineWithAuthor") {

        test("부모 댓글에 작성자 닉네임이 매핑된 CommentWithAuthor를 반환한다") {
            // Given
            val parent = CommentFixture.create(id = 1L, authorEmail = "parent@example.com")
            val grouped = CommentWithPreviewReplies(
                parent = parent,
                previewReplies = Replies(emptyList()),
                remainingReplyCount = 0,
            )
            val members = Members(
                listOf(MemberFixture.create(email = parent.authorEmail.value, nickname = "부모작성자"))
            )

            // When
            val result = grouped.combineWithAuthor(members, LikeCounts.from(emptyMap()))

            // Then
            result.comment shouldBe parent
            result.nickname shouldBe "부모작성자"
            result.replies shouldHaveSize 0
            result.remainingReplyCount shouldBe 0
        }

        test("대댓글의 작성자 닉네임도 함께 매핑된다") {
            // Given
            val parent = CommentFixture.create(id = 1L, authorEmail = "parent@example.com")
            val reply1 = CommentFixture.create(id = 2L, authorEmail = "reply1@example.com", parentCommentId = parent.id)
            val reply2 = CommentFixture.create(id = 3L, authorEmail = "reply2@example.com", parentCommentId = parent.id)
            val grouped = CommentWithPreviewReplies(
                parent = parent,
                previewReplies = Replies(listOf(reply1, reply2)),
                remainingReplyCount = 0,
            )
            val members = Members(
                listOf(
                    MemberFixture.create(email = parent.authorEmail.value, nickname = "부모작성자"),
                    MemberFixture.create(email = reply1.authorEmail.value, nickname = "대댓글작성자1"),
                    MemberFixture.create(email = reply2.authorEmail.value, nickname = "대댓글작성자2"),
                )
            )

            // When
            val result = grouped.combineWithAuthor(members, LikeCounts.from(emptyMap()))

            // Then
            result.nickname shouldBe "부모작성자"
            result.replies shouldHaveSize 2
            result.replies[0].nickname shouldBe "대댓글작성자1"
            result.replies[1].nickname shouldBe "대댓글작성자2"
        }

        test("remainingReplyCount가 올바르게 전달된다") {
            // Given
            val parent = CommentFixture.create(id = 1L, authorEmail = "parent@example.com")
            val grouped = CommentWithPreviewReplies(
                parent = parent,
                previewReplies = Replies(emptyList()),
                remainingReplyCount = 5,
            )
            val members = Members(
                listOf(MemberFixture.create(email = parent.authorEmail.value, nickname = "작성자"))
            )

            // When
            val result = grouped.combineWithAuthor(members, LikeCounts.from(emptyMap()))

            // Then
            result.remainingReplyCount shouldBe 5
        }

        test("탈퇴한 회원의 닉네임은 '알 수 없음'으로 표시된다") {
            // Given
            val parent = CommentFixture.create(id = 1L, authorEmail = "deleted@example.com")
            val reply = CommentFixture.create(id = 2L, authorEmail = "also-deleted@example.com", parentCommentId = parent.id)
            val grouped = CommentWithPreviewReplies(
                parent = parent,
                previewReplies = Replies(listOf(reply)),
                remainingReplyCount = 0,
            )
            val members = Members(emptyList())

            // When
            val result = grouped.combineWithAuthor(members, LikeCounts.from(emptyMap()))

            // Then
            result.nickname shouldBe "알 수 없음"
            result.replies[0].nickname shouldBe "알 수 없음"
        }
    }
})
