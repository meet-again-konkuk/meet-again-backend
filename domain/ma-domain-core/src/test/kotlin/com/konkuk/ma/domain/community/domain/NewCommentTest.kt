package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.fixture.NewCommentFixture
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class NewCommentTest : FunSpec({

    context("NewComment 객체 생성 테스트") {

        test("유효한 내용으로 객체 생성에 성공한다") {
            val newComment = NewCommentFixture.create()

            newComment.content shouldBe "테스트 댓글 내용입니다."
            newComment.postId shouldBe 1L
            newComment.authorId shouldBe 1L
        }

        test("내용이 500자인 경우 객체 생성에 성공한다") {
            val content = "가".repeat(500)

            val newComment = NewCommentFixture.create(content = content)

            newComment.content shouldBe content
        }

        test("내용이 빈 문자열이면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                NewCommentFixture.create(content = "")
            }
        }

        test("내용이 공백만 포함하면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                NewCommentFixture.create(content = "   ")
            }
        }

        test("내용이 500자를 초과하면 예외가 발생한다") {
            val content = "가".repeat(501)

            shouldThrow<InvalidValueException> {
                NewCommentFixture.create(content = content)
            }
        }
    }

    context("hasParent") {

        test("parentCommentId가 null이면 false를 반환한다") {
            val newComment = NewCommentFixture.create(parentCommentId = null)

            newComment.hasParent().shouldBeFalse()
        }

        test("parentCommentId가 존재하면 true를 반환한다") {
            val newComment = NewCommentFixture.create(parentCommentId = 10L)

            newComment.hasParent().shouldBeTrue()
        }
    }
})
