package com.konkuk.ma.domain.support.domain

import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NewInquiryTest : FunSpec({

    context("NewInquiry 생성") {

        test("유효한 값으로 정상 생성한다") {
            val inquiry = NewInquiry(
                authorId = 1L,
                title = "문의 제목",
                content = "문의 내용입니다.",
            )

            inquiry.authorId shouldBe 1L
            inquiry.title shouldBe "문의 제목"
            inquiry.content shouldBe "문의 내용입니다."
        }

        test("제목이 50자일 때 정상 생성한다") {
            val title = "가".repeat(NewInquiry.MAX_TITLE_LENGTH)

            val inquiry = NewInquiry(
                authorId = 1L,
                title = title,
                content = "문의 내용",
            )

            inquiry.title shouldBe title
        }

        test("내용이 500자일 때 정상 생성한다") {
            val content = "가".repeat(NewInquiry.MAX_CONTENT_LENGTH)

            val inquiry = NewInquiry(
                authorId = 1L,
                title = "문의 제목",
                content = content,
            )

            inquiry.content shouldBe content
        }

        test("제목이 비어있으면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                NewInquiry(
                    authorId = 1L,
                    title = "",
                    content = "문의 내용",
                )
            }
        }

        test("제목이 공백만 있으면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                NewInquiry(
                    authorId = 1L,
                    title = "   ",
                    content = "문의 내용",
                )
            }
        }

        test("제목이 50자를 초과하면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                NewInquiry(
                    authorId = 1L,
                    title = "가".repeat(NewInquiry.MAX_TITLE_LENGTH + 1),
                    content = "문의 내용",
                )
            }
        }

        test("내용이 비어있으면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                NewInquiry(
                    authorId = 1L,
                    title = "문의 제목",
                    content = "",
                )
            }
        }

        test("내용이 공백만 있으면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                NewInquiry(
                    authorId = 1L,
                    title = "문의 제목",
                    content = "   ",
                )
            }
        }

        test("내용이 500자를 초과하면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                NewInquiry(
                    authorId = 1L,
                    title = "문의 제목",
                    content = "가".repeat(NewInquiry.MAX_CONTENT_LENGTH + 1),
                )
            }
        }
    }
})
