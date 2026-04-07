package com.konkuk.ma.domain.common.domain.page

import com.konkuk.ma.domain.community.fixture.PostFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class CursorResultTest : FunSpec({

    context("CursorResult.of") {

        test("데이터 개수가 size와 같으면 hasNext가 true이고 nextCursorId를 반환한다") {
            val posts = List(20) { PostFixture.create(id = (20 - it).toLong()) }

            val result = CursorResult.of(posts, 20) { it.id }

            result.hasNext.shouldBeTrue()
            result.data shouldHaveSize 20
            result.nextCursorId shouldBe 1L
        }

        test("데이터 개수가 size보다 적으면 hasNext가 false이고 nextCursorId가 null이다") {
            val posts = List(10) { PostFixture.create(id = (10 - it).toLong()) }

            val result = CursorResult.of(posts, 20) { it.id }

            result.hasNext.shouldBeFalse()
            result.data shouldHaveSize 10
            result.nextCursorId.shouldBeNull()
        }

        test("빈 목록이면 hasNext가 false이고 nextCursorId가 null이다") {
            val result = CursorResult.of(emptyList<Long>(), 20) { it }

            result.hasNext.shouldBeFalse()
            result.data shouldHaveSize 0
            result.nextCursorId.shouldBeNull()
        }
    }
})
