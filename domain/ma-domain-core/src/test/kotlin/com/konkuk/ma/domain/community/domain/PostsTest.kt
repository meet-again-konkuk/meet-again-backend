package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.fixture.PostFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class PostsTest : FunSpec({

    context("hasNext") {

        test("다음 페이지가 존재하면 true를 반환한다") {
            val posts = Posts(
                data = List(Posts.PAGE_SIZE) { PostFixture.create(id = it.toLong()) },
                totalCount = Posts.PAGE_SIZE.toLong() + 1,
                currentPage = 0,
            )

            posts.hasNext().shouldBeTrue()
        }

        test("현재 페이지가 마지막이면 false를 반환한다") {
            val posts = Posts(
                data = List(Posts.PAGE_SIZE) { PostFixture.create(id = it.toLong()) },
                totalCount = Posts.PAGE_SIZE.toLong(),
                currentPage = 0,
            )

            posts.hasNext().shouldBeFalse()
        }

        test("게시글이 없으면 false를 반환한다") {
            val posts = Posts(
                data = emptyList(),
                totalCount = 0,
                currentPage = 0,
            )

            posts.hasNext().shouldBeFalse()
        }

        test("두 번째 페이지에서 다음 페이지가 없으면 false를 반환한다") {
            val totalCount = (Posts.PAGE_SIZE * 2).toLong()
            val posts = Posts(
                data = List(Posts.PAGE_SIZE) { PostFixture.create(id = it.toLong()) },
                totalCount = totalCount,
                currentPage = 1,
            )

            posts.hasNext().shouldBeFalse()
        }

        test("두 번째 페이지에서 다음 페이지가 존재하면 true를 반환한다") {
            val totalCount = (Posts.PAGE_SIZE * 2).toLong() + 1
            val posts = Posts(
                data = List(Posts.PAGE_SIZE) { PostFixture.create(id = it.toLong()) },
                totalCount = totalCount,
                currentPage = 1,
            )

            posts.hasNext().shouldBeTrue()
        }
    }
})
