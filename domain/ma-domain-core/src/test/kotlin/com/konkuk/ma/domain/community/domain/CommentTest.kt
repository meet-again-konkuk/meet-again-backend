package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.domain.community.exception.NotRootCommentException
import com.konkuk.ma.domain.community.exception.ReplyDepthExceededException
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

    context("validateCanBeParent") {

        test("일반 댓글이면 예외 없이 통과한다") {
            val comment = CommentFixture.create(parentCommentId = null)

            shouldNotThrow<ReplyDepthExceededException> {
                comment.validateCanBeParent()
            }
        }

        test("대댓글이면 ReplyDepthExceededException이 발생한다") {
            val comment = CommentFixture.create(id = 10L, parentCommentId = 5L)

            shouldThrow<ReplyDepthExceededException> {
                comment.validateCanBeParent()
            }
        }
    }

    context("validateIsRootComment") {

        test("루트 댓글이면 예외 없이 통과한다") {
            val comment = CommentFixture.create(parentCommentId = null)

            shouldNotThrow<NotRootCommentException> {
                comment.validateIsRootComment()
            }
        }

        test("대댓글이면 NotRootCommentException이 발생한다") {
            val comment = CommentFixture.create(id = 10L, parentCommentId = 5L)

            shouldThrow<NotRootCommentException> {
                comment.validateIsRootComment()
            }
        }
    }

    context("validateOwnership") {

        test("본인 이메일이면 예외 없이 통과한다") {
            val comment = CommentFixture.create()

            comment.validateOwnership(comment.authorEmail)
        }

        test("다른 이메일이면 AccessDeniedException이 발생한다") {
            val comment = CommentFixture.create()
            val otherEmail = Email("other@example.com")

            shouldThrow<AccessDeniedException> {
                comment.validateOwnership(otherEmail)
            }
        }
    }
})
