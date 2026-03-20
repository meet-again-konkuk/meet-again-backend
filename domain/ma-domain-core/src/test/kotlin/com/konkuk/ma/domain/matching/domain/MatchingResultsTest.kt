package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class MatchingResultsTest : FunSpec({

    context("targetInfoIds") {

        test("중복 없이 targetInfoId 목록을 반환한다") {
            val results = MatchingResults(
                listOf(
                    MatchingResultFixture.create(targetInfoId = 1L, targetEmail = "a@a.com"),
                    MatchingResultFixture.create(targetInfoId = 2L, targetEmail = "b@b.com"),
                    MatchingResultFixture.create(targetInfoId = 1L, targetEmail = "c@c.com")
                )
            )

            results.targetInfoIds() shouldContainExactlyInAnyOrder listOf(1L, 2L)
        }

        test("빈 리스트이면 빈 목록을 반환한다") {
            val results = MatchingResults(emptyList())

            results.targetInfoIds() shouldHaveSize 0
        }
    }

    context("filterNew") {

        test("기존에 없는 결과만 필터링한다") {
            val existing = MatchingResults(
                listOf(
                    MatchingResultFixture.create(targetInfoId = 1L, targetEmail = "a@a.com")
                )
            )

            val candidates = MatchingResults(
                listOf(
                    MatchingResultFixture.create(targetInfoId = 1L, targetEmail = "a@a.com"),
                    MatchingResultFixture.create(targetInfoId = 2L, targetEmail = "b@b.com")
                )
            )

            val newResults = candidates.filterNew(existing)

            newResults.data shouldHaveSize 1
            newResults.data.first().targetInfoId shouldBe 2L
            newResults.data.first().targetEmail shouldBe "b@b.com"
        }

        test("모두 기존에 존재하면 빈 결과를 반환한다") {
            val existing = MatchingResults(
                listOf(
                    MatchingResultFixture.create(targetInfoId = 1L, targetEmail = "a@a.com"),
                    MatchingResultFixture.create(targetInfoId = 2L, targetEmail = "b@b.com")
                )
            )

            val candidates = MatchingResults(
                listOf(
                    MatchingResultFixture.create(targetInfoId = 1L, targetEmail = "a@a.com")
                )
            )

            val newResults = candidates.filterNew(existing)

            newResults.data shouldHaveSize 0
        }

        test("기존 결과가 비어있으면 모든 후보를 반환한다") {
            val existing = MatchingResults(emptyList())

            val candidates = MatchingResults(
                listOf(
                    MatchingResultFixture.create(targetInfoId = 1L, targetEmail = "a@a.com"),
                    MatchingResultFixture.create(targetInfoId = 2L, targetEmail = "b@b.com")
                )
            )

            val newResults = candidates.filterNew(existing)

            newResults.data shouldHaveSize 2
        }

        test("같은 targetInfoId라도 targetEmail이 다르면 새로운 결과로 판단한다") {
            val existing = MatchingResults(
                listOf(
                    MatchingResultFixture.create(targetInfoId = 1L, targetEmail = "a@a.com")
                )
            )

            val candidates = MatchingResults(
                listOf(
                    MatchingResultFixture.create(targetInfoId = 1L, targetEmail = "different@a.com")
                )
            )

            val newResults = candidates.filterNew(existing)

            newResults.data shouldHaveSize 1
        }
    }

    context("merge") {

        test("여러 MatchingResults를 하나로 합친다") {
            val results1 = MatchingResults(
                listOf(MatchingResultFixture.create(targetInfoId = 1L, targetEmail = "a@a.com"))
            )
            val results2 = MatchingResults(
                listOf(MatchingResultFixture.create(targetInfoId = 2L, targetEmail = "b@b.com"))
            )

            val merged = MatchingResults.merge(listOf(results1, results2))

            merged.data shouldHaveSize 2
        }

        test("빈 리스트를 merge하면 빈 결과를 반환한다") {
            val merged = MatchingResults.merge(emptyList())

            merged.data shouldHaveSize 0
        }

        test("하나의 MatchingResults만 merge하면 동일한 결과를 반환한다") {
            val results = MatchingResults(
                listOf(
                    MatchingResultFixture.create(targetInfoId = 1L, targetEmail = "a@a.com"),
                    MatchingResultFixture.create(targetInfoId = 2L, targetEmail = "b@b.com")
                )
            )

            val merged = MatchingResults.merge(listOf(results))

            merged.data shouldHaveSize 2
        }
    }
})
