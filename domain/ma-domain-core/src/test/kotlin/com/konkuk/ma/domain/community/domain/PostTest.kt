package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.fixture.PostFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class PostTest : FunSpec({

    context("calculateTimeAgo") {

        val baseTime = LocalDateTime.of(2026, 4, 6, 12, 0, 0)

        test("1분 미만이면 '방금 전'을 반환한다") {
            val post = PostFixture.create(createdDate = baseTime.minusSeconds(30))

            post.calculateTimeAgo(baseTime) shouldBe "방금 전"
        }

        test("정확히 0초 차이면 '방금 전'을 반환한다") {
            val post = PostFixture.create(createdDate = baseTime)

            post.calculateTimeAgo(baseTime) shouldBe "방금 전"
        }

        test("1분 이상 1시간 미만이면 '분 전'을 반환한다") {
            val post = PostFixture.create(createdDate = baseTime.minusMinutes(30))

            post.calculateTimeAgo(baseTime) shouldBe "30분 전"
        }

        test("정확히 1분이면 '1분 전'을 반환한다") {
            val post = PostFixture.create(createdDate = baseTime.minusMinutes(1))

            post.calculateTimeAgo(baseTime) shouldBe "1분 전"
        }

        test("1시간 이상 1일 미만이면 '시간 전'을 반환한다") {
            val post = PostFixture.create(createdDate = baseTime.minusHours(5))

            post.calculateTimeAgo(baseTime) shouldBe "5시간 전"
        }

        test("정확히 1시간이면 '1시간 전'을 반환한다") {
            val post = PostFixture.create(createdDate = baseTime.minusHours(1))

            post.calculateTimeAgo(baseTime) shouldBe "1시간 전"
        }

        test("1일 이상 30일 미만이면 '일 전'을 반환한다") {
            val post = PostFixture.create(createdDate = baseTime.minusDays(7))

            post.calculateTimeAgo(baseTime) shouldBe "7일 전"
        }

        test("정확히 1일이면 '1일 전'을 반환한다") {
            val post = PostFixture.create(createdDate = baseTime.minusDays(1))

            post.calculateTimeAgo(baseTime) shouldBe "1일 전"
        }

        test("30일 이상 365일 미만이면 '개월 전'을 반환한다") {
            val post = PostFixture.create(createdDate = baseTime.minusDays(90))

            post.calculateTimeAgo(baseTime) shouldBe "3개월 전"
        }

        test("정확히 30일이면 '1개월 전'을 반환한다") {
            val post = PostFixture.create(createdDate = baseTime.minusDays(30))

            post.calculateTimeAgo(baseTime) shouldBe "1개월 전"
        }

        test("365일 이상이면 '년 전'을 반환한다") {
            val post = PostFixture.create(createdDate = baseTime.minusDays(730))

            post.calculateTimeAgo(baseTime) shouldBe "2년 전"
        }

        test("정확히 365일이면 '1년 전'을 반환한다") {
            val post = PostFixture.create(createdDate = baseTime.minusDays(365))

            post.calculateTimeAgo(baseTime) shouldBe "1년 전"
        }
    }
})
