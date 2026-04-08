package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.fixture.CommentFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class RootCommentsTest : FunSpec({

    context("groupWith") {

        test("부모 댓글에 대댓글이 올바르게 그룹핑된다") {
            // Given
            val parent = CommentFixture.create(id = 1L)
            val reply1 = CommentFixture.create(id = 2L, parentCommentId = parent.id)
            val reply2 = CommentFixture.create(id = 3L, parentCommentId = parent.id)
            val rootComments = RootComments(listOf(parent))
            val replies = Replies(listOf(reply1, reply2))

            // When
            val result = rootComments.groupWith(replies)

            // Then
            result shouldHaveSize 1
            result[0].parent shouldBe parent
            result[0].previewReplies.data shouldHaveSize 2
        }

        test("대댓글이 4개 이상이면 최신순 3개만 미리보기로 포함한다") {
            // Given
            val parent = CommentFixture.create(id = 1L)
            val now = LocalDateTime.of(2025, 1, 1, 12, 0)
            val replies = (1..5).map { i ->
                CommentFixture.create(
                    id = (i + 1).toLong(),
                    parentCommentId = parent.id,
                    createdDate = now.plusMinutes(i.toLong()),
                )
            }
            val rootComments = RootComments(listOf(parent))

            // When
            val result = rootComments.groupWith(Replies(replies))

            // Then
            result shouldHaveSize 1
            result[0].previewReplies.data shouldHaveSize 3
            result[0].remainingReplyCount shouldBe 2
        }

        test("대댓글이 없는 부모 댓글은 빈 미리보기와 remainingReplyCount 0을 반환한다") {
            // Given
            val parent = CommentFixture.create(id = 1L)
            val rootComments = RootComments(listOf(parent))
            val replies = Replies(emptyList())

            // When
            val result = rootComments.groupWith(replies)

            // Then
            result shouldHaveSize 1
            result[0].previewReplies.data shouldHaveSize 0
            result[0].remainingReplyCount shouldBe 0
        }

        test("빈 부모 댓글 목록이면 빈 결과를 반환한다") {
            // Given
            val rootComments = RootComments(emptyList())
            val replies = Replies(emptyList())

            // When
            val result = rootComments.groupWith(replies)

            // Then
            result shouldHaveSize 0
        }

        test("여러 부모 댓글에 각각의 대댓글이 올바르게 그룹핑된다") {
            // Given
            val parent1 = CommentFixture.create(id = 1L)
            val parent2 = CommentFixture.create(id = 2L)
            val replyToParent1 = CommentFixture.create(id = 3L, parentCommentId = parent1.id)
            val replyToParent2 = CommentFixture.create(id = 4L, parentCommentId = parent2.id)
            val rootComments = RootComments(listOf(parent1, parent2))
            val replies = Replies(listOf(replyToParent1, replyToParent2))

            // When
            val result = rootComments.groupWith(replies)

            // Then
            result shouldHaveSize 2
            result[0].parent shouldBe parent1
            result[0].previewReplies.data shouldHaveSize 1
            result[1].parent shouldBe parent2
            result[1].previewReplies.data shouldHaveSize 1
        }
    }
})
