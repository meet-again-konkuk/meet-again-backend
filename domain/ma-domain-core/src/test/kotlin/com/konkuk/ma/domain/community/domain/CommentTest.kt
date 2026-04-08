package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.fixture.CommentFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

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
})
