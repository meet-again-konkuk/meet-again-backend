package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.fixture.CommentFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.Members
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class CommentDetailTest : FunSpec({

    context("extractAuthorEmails") {

        test("루트 댓글과 대댓글의 이메일을 모두 추출한다") {
            // Given
            val rootComment = CommentFixture.create(authorEmail = "root@example.com")
            val reply1 = CommentFixture.create(id = 2L, authorEmail = "reply1@example.com", parentCommentId = rootComment.id)
            val reply2 = CommentFixture.create(id = 3L, authorEmail = "reply2@example.com", parentCommentId = rootComment.id)
            val commentDetail = CommentDetail(rootComment, Replies(listOf(reply1, reply2)))

            // When
            val result = commentDetail.extractAuthorEmails()

            // Then
            result shouldContainExactlyInAnyOrder setOf(
                rootComment.authorEmail,
                reply1.authorEmail,
                reply2.authorEmail,
            )
        }

        test("대댓글 작성자와 루트 댓글 작성자가 같으면 중복 없이 추출한다") {
            // Given
            val sameEmail = "same@example.com"
            val rootComment = CommentFixture.create(authorEmail = sameEmail)
            val reply = CommentFixture.create(id = 2L, authorEmail = sameEmail, parentCommentId = rootComment.id)
            val commentDetail = CommentDetail(rootComment, Replies(listOf(reply)))

            // When
            val result = commentDetail.extractAuthorEmails()

            // Then
            result shouldHaveSize 1
            result shouldContainExactlyInAnyOrder setOf(Email(sameEmail))
        }

        test("대댓글이 없으면 루트 댓글 이메일만 추출한다") {
            // Given
            val rootComment = CommentFixture.create(authorEmail = "root@example.com")
            val commentDetail = CommentDetail(rootComment, Replies(emptyList()))

            // When
            val result = commentDetail.extractAuthorEmails()

            // Then
            result shouldHaveSize 1
            result shouldContainExactlyInAnyOrder setOf(rootComment.authorEmail)
        }
    }

    context("combineWithAuthors") {

        test("루트 댓글과 대댓글에 닉네임을 매핑하여 반환한다") {
            // Given
            val rootComment = CommentFixture.create(authorEmail = "root@example.com")
            val reply1 = CommentFixture.create(id = 2L, authorEmail = "reply1@example.com", parentCommentId = rootComment.id)
            val reply2 = CommentFixture.create(id = 3L, authorEmail = "reply2@example.com", parentCommentId = rootComment.id)
            val commentDetail = CommentDetail(rootComment, Replies(listOf(reply1, reply2)))

            val rootMember = MemberFixture.create(email = rootComment.authorEmail.value, nickname = "루트작성자")
            val reply1Member = MemberFixture.create(email = reply1.authorEmail.value, nickname = "대댓글작성자1")
            val reply2Member = MemberFixture.create(email = reply2.authorEmail.value, nickname = "대댓글작성자2")
            val members = Members(listOf(rootMember, reply1Member, reply2Member))

            // When
            val result = commentDetail.combineWithAuthor(members, LikeCounts.from(emptyMap()))

            // Then
            result.comment shouldBe rootComment
            result.nickname shouldBe rootMember.nickname
            result.replies shouldHaveSize 2
            result.replies[0].nickname shouldBe reply1Member.nickname
            result.replies[1].nickname shouldBe reply2Member.nickname
            result.remainingReplyCount shouldBe 0
        }

        test("대댓글이 없으면 빈 대댓글 목록과 remainingReplyCount 0을 반환한다") {
            // Given
            val rootComment = CommentFixture.create(authorEmail = "root@example.com")
            val commentDetail = CommentDetail(rootComment, Replies(emptyList()))

            val rootMember = MemberFixture.create(email = rootComment.authorEmail.value, nickname = "루트작성자")
            val members = Members(listOf(rootMember))

            // When
            val result = commentDetail.combineWithAuthor(members, LikeCounts.from(emptyMap()))

            // Then
            result.comment shouldBe rootComment
            result.nickname shouldBe rootMember.nickname
            result.replies shouldHaveSize 0
            result.remainingReplyCount shouldBe 0
        }

        test("닉네임을 찾을 수 없으면 '알 수 없음'으로 표시한다") {
            // Given
            val rootComment = CommentFixture.create(authorEmail = "unknown@example.com")
            val commentDetail = CommentDetail(rootComment, Replies(emptyList()))
            val members = Members(emptyList())

            // When
            val result = commentDetail.combineWithAuthor(members, LikeCounts.from(emptyMap()))

            // Then
            result.nickname shouldBe "알 수 없음"
        }
    }
})
