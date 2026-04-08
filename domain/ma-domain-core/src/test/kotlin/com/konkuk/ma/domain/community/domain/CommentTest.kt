package com.konkuk.ma.domain.community.domain

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
            }.message shouldBe "대댓글에는 답글을 달 수 없습니다."
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
            }.message shouldBe "루트 댓글만 조회할 수 있습니다."
        }
    }
})
