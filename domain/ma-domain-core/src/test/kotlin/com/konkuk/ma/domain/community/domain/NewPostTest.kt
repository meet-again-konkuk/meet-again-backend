package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.fixture.NewPostFixture
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

        test("제목이 40자인 경우 객체 생성에 성공한다") {
            val title = "가".repeat(40)

            val newPost = NewPostFixture.create(title = title)

            newPost.title shouldBe title
        }

        test("제목이 빈 문자열이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                NewPostFixture.create(title = "")
            }.message shouldBe "게시글 제목은 비어있을 수 없습니다."
        }

        test("제목이 공백만 포함하면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                NewPostFixture.create(title = "   ")
            }.message shouldBe "게시글 제목은 비어있을 수 없습니다."
        }

        test("제목이 40자를 초과하면 예외가 발생한다") {
            val title = "가".repeat(41)

            shouldThrow<IllegalArgumentException> {
                NewPostFixture.create(title = title)
            }.message shouldBe "게시글 제목은 40자 이하여야 합니다."
        }

        test("내용이 빈 문자열이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                NewPostFixture.create(content = "")
            }.message shouldBe "게시글 내용은 비어있을 수 없습니다."
        }

        test("내용이 공백만 포함하면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                NewPostFixture.create(content = "   ")
            }.message shouldBe "게시글 내용은 비어있을 수 없습니다."
        }
    }
})
