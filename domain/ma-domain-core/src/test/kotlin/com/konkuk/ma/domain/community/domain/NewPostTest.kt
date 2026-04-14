package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.fixture.NewPostFixture
import com.konkuk.ma.domain.common.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NewPostTest : FunSpec({

    context("NewPost 객체 생성 테스트") {

        test("유효한 제목과 내용으로 객체 생성에 성공한다") {
            val newPost = NewPostFixture.create()

            newPost.title shouldBe "테스트 게시글"
            newPost.content shouldBe "테스트 내용입니다."
            newPost.category shouldBe PostCategory.SUCCESS_STORY
        }

        test("제목이 30자인 경우 객체 생성에 성공한다") {
            val title = "가".repeat(30)

            val newPost = NewPostFixture.create(title = title)

            newPost.title shouldBe title
        }

        test("제목이 빈 문자열이면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                NewPostFixture.create(title = "")
            }
        }

        test("제목이 공백만 포함하면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                NewPostFixture.create(title = "   ")
            }
        }

        test("제목이 30자를 초과하면 예외가 발생한다") {
            val title = "가".repeat(31)

            shouldThrow<InvalidValueException> {
                NewPostFixture.create(title = title)
            }
        }

        test("내용이 빈 문자열이면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                NewPostFixture.create(content = "")
            }
        }

        test("내용이 공백만 포함하면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                NewPostFixture.create(content = "   ")
            }
        }

        test("내용이 2000자인 경우 객체 생성에 성공한다") {
            val content = "가".repeat(2000)

            val newPost = NewPostFixture.create(content = content)

            newPost.content shouldBe content
        }

        test("내용이 2000자를 초과하면 예외가 발생한다") {
            val content = "가".repeat(2001)

            shouldThrow<InvalidValueException> {
                NewPostFixture.create(content = content)
            }
        }
    }
})
