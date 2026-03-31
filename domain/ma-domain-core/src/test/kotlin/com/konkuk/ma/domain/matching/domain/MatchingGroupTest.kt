package com.konkuk.ma.domain.matching.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MatchingGroupTest : FunSpec({

    context("Phone") {

        context("isFullMatched") {
            test("중간번호와 끝번호 모두 일치하면 true를 반환한다") {
                val phone = MatchingGroup.Phone(middleNumberMatched = true, lastNumberMatched = true)

                phone.isFullMatched() shouldBe true
            }

            test("중간번호만 일치하면 false를 반환한다") {
                val phone = MatchingGroup.Phone(middleNumberMatched = true, lastNumberMatched = false)

                phone.isFullMatched() shouldBe false
            }

            test("끝번호만 일치하면 false를 반환한다") {
                val phone = MatchingGroup.Phone(middleNumberMatched = false, lastNumberMatched = true)

                phone.isFullMatched() shouldBe false
            }

            test("아무것도 일치하지 않으면 false를 반환한다") {
                val phone = MatchingGroup.Phone(middleNumberMatched = false, lastNumberMatched = false)

                phone.isFullMatched() shouldBe false
            }
        }

        context("calculatePartialRate") {
            test("중간번호만 일치하면 30을 반환한다") {
                val phone = MatchingGroup.Phone(middleNumberMatched = true, lastNumberMatched = false)

                phone.calculatePartialRate() shouldBe 30
            }

            test("끝번호만 일치하면 30을 반환한다") {
                val phone = MatchingGroup.Phone(middleNumberMatched = false, lastNumberMatched = true)

                phone.calculatePartialRate() shouldBe 30
            }

            test("둘 다 일치하면 30을 반환한다") {
                val phone = MatchingGroup.Phone(middleNumberMatched = true, lastNumberMatched = true)

                phone.calculatePartialRate() shouldBe 30
            }

            test("아무것도 일치하지 않으면 0을 반환한다") {
                val phone = MatchingGroup.Phone(middleNumberMatched = false, lastNumberMatched = false)

                phone.calculatePartialRate() shouldBe 0
            }
        }
    }

    context("Birth") {

        context("isFullMatched") {
            test("연도, 월, 일 모두 일치하면 true를 반환한다") {
                val birth = MatchingGroup.Birth(yearMatched = true, monthMatched = true, dayMatched = true)

                birth.isFullMatched() shouldBe true
            }

            test("하나라도 일치하지 않으면 false를 반환한다") {
                val birth = MatchingGroup.Birth(yearMatched = true, monthMatched = true, dayMatched = false)

                birth.isFullMatched() shouldBe false
            }

            test("아무것도 일치하지 않으면 false를 반환한다") {
                val birth = MatchingGroup.Birth(yearMatched = false, monthMatched = false, dayMatched = false)

                birth.isFullMatched() shouldBe false
            }
        }

        context("calculatePartialRate") {
            test("3개 모두 일치하면 30을 반환한다") {
                val birth = MatchingGroup.Birth(yearMatched = true, monthMatched = true, dayMatched = true)

                birth.calculatePartialRate() shouldBe 30
            }

            test("2개 일치하면 20을 반환한다") {
                val birth = MatchingGroup.Birth(yearMatched = true, monthMatched = true, dayMatched = false)

                birth.calculatePartialRate() shouldBe 20
            }

            test("1개 일치하면 10을 반환한다") {
                val birth = MatchingGroup.Birth(yearMatched = false, monthMatched = false, dayMatched = true)

                birth.calculatePartialRate() shouldBe 10
            }

            test("아무것도 일치하지 않으면 0을 반환한다") {
                val birth = MatchingGroup.Birth(yearMatched = false, monthMatched = false, dayMatched = false)

                birth.calculatePartialRate() shouldBe 0
            }
        }
    }
})
