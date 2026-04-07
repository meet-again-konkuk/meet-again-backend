package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.fixture.PostFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class PostsTest : FunSpec({

    context("CursorResult.of") {

        test("데이터가 size보다 많으면 hasNext가 true이고 nextCursorId를 반환한다") {
            val posts = List(21) { PostFixture.create(id = (21 - it).toLong()) }

            val result = CursorResult.of(posts, 20) { it.id }

            result.hasNext.shouldBeTrue()
            result.data shouldHaveSize 20
            result.nextCursorId shouldBe 2L
        }

        test("데이터가 size 이하이면 hasNext가 false이고 nextCursorId가 null이다") {
            val posts = List(20) { PostFixture.create(id = (20 - it).toLong()) }

            val result = CursorResult.of(posts, 20) { it.id }

            result.hasNext.shouldBeFalse()
            result.data shouldHaveSize 20
            result.nextCursorId.shouldBeNull()
        }

        test("빈 목록이면 hasNext가 false이고 nextCursorId가 null이다") {
            val result = CursorResult.of(emptyList<Post>(), 20) { it.id }

            result.hasNext.shouldBeFalse()
            result.data shouldHaveSize 0
            result.nextCursorId.shouldBeNull()
        }
    }
})
