package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.fixture.CommentFixture
import com.konkuk.ma.domain.community.fixture.ViewerFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.Members
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class CommentsTest : FunSpec({

    context("extractAuthorIds") {

        test("부모 댓글과 대댓글의 작성자 id를 모두 추출한다") {
            // Given
            val parentComment = CommentFixture.create(id = 1L, authorId = 10L)
            val reply1 = CommentFixture.create(id = 2L, authorId = 20L, parentCommentId = 1L)
            val reply2 = CommentFixture.create(id = 3L, authorId = 30L, parentCommentId = 1L)
            val comments = Comments(listOf(parentComment, reply1, reply2))

            // When
            val result = comments.extractAuthorIds()

            // Then
            result shouldContainExactlyInAnyOrder setOf(
                parentComment.authorId,
                reply1.authorId,
                reply2.authorId,
            )
        }

        test("동일한 작성자가 여러 댓글을 작성해도 id가 중복되지 않는다") {
            // Given
            val sameAuthorId = 10L
            val comment1 = CommentFixture.create(id = 1L, authorId = sameAuthorId)
            val comment2 = CommentFixture.create(id = 2L, authorId = sameAuthorId, parentCommentId = 1L)
            val comments = Comments(listOf(comment1, comment2))

            // When
            val result = comments.extractAuthorIds()

            // Then
            result shouldHaveSize 1
            result.first() shouldBe sameAuthorId
        }

        test("댓글이 없으면 빈 Set을 반환한다") {
            // Given
            val comments = Comments(emptyList())

            // When
            val result = comments.extractAuthorIds()

            // Then
            result shouldHaveSize 0
        }
    }

    context("combineWithAuthors") {

        test("부모 댓글에 대댓글 3개까지 미리보기로 포함한다") {
            // Given
            val parentComment = CommentFixture.create(id = 1L, authorId = 10L)
            val replies = (1..3).map { i ->
                CommentFixture.create(
                    id = (i + 1).toLong(),
                    authorId = (i + 1) * 10L,
                    parentCommentId = parentComment.id,
                )
            }
            val comments = Comments(listOf(parentComment) + replies)

            val members = Members(
                listOf(
                    MemberFixture.create(id = parentComment.authorId, nickname = "부모작성자"),
                ) + replies.mapIndexed { index, reply ->
                    MemberFixture.create(id = reply.authorId, nickname = "대댓글작성자${index + 1}")
                }
            )

            // When
            val result = comments.groupByRootComment(members, LikeCounts.from(emptyMap()), ViewerFixture.create())

            // Then
            result shouldHaveSize 1
            result[0].nickname shouldBe "부모작성자"
            result[0].replies shouldHaveSize 3
            result[0].remainingReplyCount shouldBe 0
        }

        test("대댓글이 3개를 초과하면 remainingReplyCount를 반환한다") {
            // Given
            val parentComment = CommentFixture.create(id = 1L, authorId = 10L)
            val replies = (1..5).map { i ->
                CommentFixture.create(
                    id = (i + 1).toLong(),
                    authorId = (i + 1) * 10L,
                    parentCommentId = parentComment.id,
                )
            }
            val comments = Comments(listOf(parentComment) + replies)

            val members = Members(
                listOf(
                    MemberFixture.create(id = parentComment.authorId, nickname = "부모작성자"),
                ) + replies.map { reply ->
                    MemberFixture.create(id = reply.authorId, nickname = "닉네임")
                }
            )

            // When
            val result = comments.groupByRootComment(members, LikeCounts.from(emptyMap()), ViewerFixture.create())

            // Then
            result shouldHaveSize 1
            result[0].replies shouldHaveSize 3
            result[0].remainingReplyCount shouldBe 2
        }

        test("대댓글이 없는 댓글은 빈 replies와 remainingReplyCount 0을 반환한다") {
            // Given
            val parentComment = CommentFixture.create(id = 1L, authorId = 10L)
            val comments = Comments(listOf(parentComment))

            val members = Members(
                listOf(MemberFixture.create(id = parentComment.authorId, nickname = "작성자"))
            )

            // When
            val result = comments.groupByRootComment(members, LikeCounts.from(emptyMap()), ViewerFixture.create())

            // Then
            result shouldHaveSize 1
            result[0].replies shouldHaveSize 0
            result[0].remainingReplyCount shouldBe 0
        }

        test("탈퇴 회원의 닉네임은 '알 수 없음'으로 표시된다") {
            // Given
            val parentComment = CommentFixture.create(id = 1L, authorId = 10L)
            val reply = CommentFixture.create(
                id = 2L,
                authorId = 20L,
                parentCommentId = parentComment.id,
            )
            val comments = Comments(listOf(parentComment, reply))

            val members = Members(emptyList())

            // When
            val result = comments.groupByRootComment(members, LikeCounts.from(emptyMap()), ViewerFixture.create())

            // Then
            result shouldHaveSize 1
            result[0].nickname shouldBe "알 수 없음"
            result[0].replies[0].nickname shouldBe "알 수 없음"
        }

        test("댓글이 없으면 빈 목록을 반환한다") {
            // Given
            val comments = Comments(emptyList())
            val members = Members(emptyList())

            // When
            val result = comments.groupByRootComment(members, LikeCounts.from(emptyMap()), ViewerFixture.create())

            // Then
            result shouldHaveSize 0
        }

        test("여러 부모 댓글에 각각 대댓글이 있으면 개별적으로 조합한다") {
            // Given
            val parent1 = CommentFixture.create(id = 1L, authorId = 10L)
            val parent2 = CommentFixture.create(id = 2L, authorId = 20L)
            val replyToParent1 = CommentFixture.create(
                id = 3L,
                authorId = 30L,
                parentCommentId = parent1.id,
            )
            val replyToParent2 = CommentFixture.create(
                id = 4L,
                authorId = 40L,
                parentCommentId = parent2.id,
            )
            val comments = Comments(listOf(parent1, parent2, replyToParent1, replyToParent2))

            val members = Members(
                listOf(
                    MemberFixture.create(id = parent1.authorId, nickname = "부모1"),
                    MemberFixture.create(id = parent2.authorId, nickname = "부모2"),
                    MemberFixture.create(id = replyToParent1.authorId, nickname = "대댓글1"),
                    MemberFixture.create(id = replyToParent2.authorId, nickname = "대댓글2"),
                )
            )

            // When
            val result = comments.groupByRootComment(members, LikeCounts.from(emptyMap()), ViewerFixture.create())

            // Then
            result shouldHaveSize 2
            result[0].nickname shouldBe "부모1"
            result[0].replies shouldHaveSize 1
            result[0].replies[0].nickname shouldBe "대댓글1"
            result[1].nickname shouldBe "부모2"
            result[1].replies shouldHaveSize 1
            result[1].replies[0].nickname shouldBe "대댓글2"
        }
    }
})
