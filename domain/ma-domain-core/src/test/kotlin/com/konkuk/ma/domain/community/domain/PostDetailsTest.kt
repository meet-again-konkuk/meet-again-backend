package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.fixture.PostDetailsFixture
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PostDetailsTest : FunSpec({

    context("PostDetails 객체 생성 테스트") {

        test("유효한 카테고리·제목·내용으로 객체 생성에 성공한다") {
            val category = PostCategory.CHEER
            val title = "수정된 제목"
            val content = "수정된 내용"

            val details = PostDetailsFixture.create(category = category, title = title, content = content)

            details.category shouldBe category
            details.title shouldBe title
            details.content shouldBe content
        }

        test("제목이 30자(경계값)이면 객체 생성에 성공한다") {
            val title = "가".repeat(PostDetails.MAX_TITLE_LENGTH)

            val details = PostDetailsFixture.create(title = title)

            details.title shouldBe title
        }

        test("내용이 2000자(경계값)이면 객체 생성에 성공한다") {
            val content = "가".repeat(PostDetails.MAX_CONTENT_LENGTH)

            val details = PostDetailsFixture.create(content = content)

            details.content shouldBe content
        }

        test("제목이 빈 문자열이면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                PostDetailsFixture.create(title = "")
            }
        }

        test("제목이 공백만 포함하면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                PostDetailsFixture.create(title = "   ")
            }
        }

        test("제목이 30자를 초과하면 InvalidValueException이 발생한다") {
            val title = "가".repeat(PostDetails.MAX_TITLE_LENGTH + 1)

            shouldThrow<InvalidValueException> {
                PostDetailsFixture.create(title = title)
            }
        }

        test("내용이 빈 문자열이면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                PostDetailsFixture.create(content = "")
            }
        }

        test("내용이 공백만 포함하면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                PostDetailsFixture.create(content = "   ")
            }
        }

        test("내용이 2000자를 초과하면 InvalidValueException이 발생한다") {
            val content = "가".repeat(PostDetails.MAX_CONTENT_LENGTH + 1)

            shouldThrow<InvalidValueException> {
                PostDetailsFixture.create(content = content)
            }
        }
    }
})
