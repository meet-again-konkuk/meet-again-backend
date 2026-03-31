package com.konkuk.ma.domain.matching.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MatchRateCalculatorTest : FunSpec({

    context("전체 일치 보너스 테이블") {

        test("전화번호 완전 + 생년월일 완전 + 지역 일치 시 99를 반환한다") {
            val calculator = MatchRateCalculator(
                groups = listOf(
                    MatchingGroup.Phone(middleNumberMatched = true, lastNumberMatched = true),
                    MatchingGroup.Birth(yearMatched = true, monthMatched = true, dayMatched = true),
                ),
                regionMatched = true,
            )

            calculator.calculate() shouldBe 99
        }

        test("전화번호 완전 + 생년월일 완전 + 지역 불일치 시 97을 반환한다") {
            val calculator = MatchRateCalculator(
                groups = listOf(
                    MatchingGroup.Phone(middleNumberMatched = true, lastNumberMatched = true),
                    MatchingGroup.Birth(yearMatched = true, monthMatched = true, dayMatched = true),
                ),
                regionMatched = false,
            )

            calculator.calculate() shouldBe 97
        }

        test("전화번호 완전 + 지역 일치 시 90을 반환한다") {
            val calculator = MatchRateCalculator(
                groups = listOf(
                    MatchingGroup.Phone(middleNumberMatched = true, lastNumberMatched = true),
                    MatchingGroup.Birth(yearMatched = false, monthMatched = false, dayMatched = false),
                ),
                regionMatched = true,
            )

            calculator.calculate() shouldBe 90
        }

        test("전화번호 완전 일치만 있으면 85를 반환한다") {
            val calculator = MatchRateCalculator(
                groups = listOf(
                    MatchingGroup.Phone(middleNumberMatched = true, lastNumberMatched = true),
                    MatchingGroup.Birth(yearMatched = false, monthMatched = false, dayMatched = false),
                ),
                regionMatched = false,
            )

            calculator.calculate() shouldBe 85
        }

        test("생년월일 완전 + 지역 일치 시 83을 반환한다") {
            val calculator = MatchRateCalculator(
                groups = listOf(
                    MatchingGroup.Phone(middleNumberMatched = false, lastNumberMatched = false),
                    MatchingGroup.Birth(yearMatched = true, monthMatched = true, dayMatched = true),
                ),
                regionMatched = true,
            )

            calculator.calculate() shouldBe 83
        }

        test("생년월일 완전 일치만 있으면 80을 반환한다") {
            val calculator = MatchRateCalculator(
                groups = listOf(
                    MatchingGroup.Phone(middleNumberMatched = false, lastNumberMatched = false),
                    MatchingGroup.Birth(yearMatched = true, monthMatched = true, dayMatched = true),
                ),
                regionMatched = false,
            )

            calculator.calculate() shouldBe 80
        }
    }

    context("부분 일치 합산") {

        test("전화번호 부분(중간번호만) + 생년월일 부분(연도+월) + 지역 일치 시 60을 반환한다") {
            val calculator = MatchRateCalculator(
                groups = listOf(
                    MatchingGroup.Phone(middleNumberMatched = true, lastNumberMatched = false),
                    MatchingGroup.Birth(yearMatched = true, monthMatched = true, dayMatched = false),
                ),
                regionMatched = true,
            )

            // Phone partial(30) + Birth partial(2*10=20) + Region(10) = 60
            calculator.calculate() shouldBe 60
        }

        test("전화번호 부분(중간번호만) + 지역 일치 시 40을 반환한다") {
            val calculator = MatchRateCalculator(
                groups = listOf(
                    MatchingGroup.Phone(middleNumberMatched = true, lastNumberMatched = false),
                    MatchingGroup.Birth(yearMatched = false, monthMatched = false, dayMatched = false),
                ),
                regionMatched = true,
            )

            calculator.calculate() shouldBe 40
        }

        test("생년월일 부분(연도+월) 일치만 있으면 20을 반환한다") {
            val calculator = MatchRateCalculator(
                groups = listOf(
                    MatchingGroup.Phone(middleNumberMatched = false, lastNumberMatched = false),
                    MatchingGroup.Birth(yearMatched = true, monthMatched = true, dayMatched = false),
                ),
                regionMatched = false,
            )

            calculator.calculate() shouldBe 20
        }

        test("아무것도 일치하지 않으면 0을 반환한다") {
            val calculator = MatchRateCalculator(
                groups = listOf(
                    MatchingGroup.Phone(middleNumberMatched = false, lastNumberMatched = false),
                    MatchingGroup.Birth(yearMatched = false, monthMatched = false, dayMatched = false),
                ),
                regionMatched = false,
            )

            calculator.calculate() shouldBe 0
        }

        test("지역만 일치하면 10을 반환한다") {
            val calculator = MatchRateCalculator(
                groups = listOf(
                    MatchingGroup.Phone(middleNumberMatched = false, lastNumberMatched = false),
                    MatchingGroup.Birth(yearMatched = false, monthMatched = false, dayMatched = false),
                ),
                regionMatched = true,
            )

            calculator.calculate() shouldBe 10
        }
    }
})
