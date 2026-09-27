package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.exception.InvalidStateException
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.domain.community.fixture.CommentFixture
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class CommentTest : FunSpec({

    context("displayContent") {

        test("삭제되지 않은 댓글은 원본 내용을 반환한다") {
            val comment = CommentFixture.create(content = "원본 내용")

            comment.displayContent() shouldBe "원본 내용"
        }

        test("삭제된 댓글은 '삭제된 댓글입니다.'를 반환한다") {
            val comment = CommentFixture.create(content = "원본 내용", deleted = true)

            comment.displayContent() shouldBe "삭제된 댓글입니다."
        }
    }

    context("hasParent") {

        test("parentCommentId가 null이면 false를 반환한다") {
            val comment = CommentFixture.create(parentCommentId = null)

            comment.hasParent().shouldBeFalse()
        }

        test("parentCommentId가 존재하면 true를 반환한다") {
            val comment = CommentFixture.create(parentCommentId = 5L)

            comment.hasParent().shouldBeTrue()
        }
    }

    context("validateIsRootComment") {

        test("루트 댓글이면 예외 없이 통과한다") {
            val comment = CommentFixture.create(parentCommentId = null)

            comment.validateIsRootComment()
        }

        test("대댓글이면 InvalidStateException이 발생한다") {
            val comment = CommentFixture.create(id = 10L, parentCommentId = 5L)

            shouldThrow<InvalidStateException> {
                comment.validateIsRootComment()
            }
        }
    }

    context("validateOwnership") {

        test("본인 memberId이면 예외 없이 통과한다") {
            val comment = CommentFixture.create()

            comment.validateOwnership(comment.authorId)
        }

        test("다른 memberId이면 AccessDeniedException이 발생한다") {
            val comment = CommentFixture.create()
            val otherMemberId = comment.authorId + 1

            shouldThrow<AccessDeniedException> {
                comment.validateOwnership(otherMemberId)
            }
        }
    }
})
