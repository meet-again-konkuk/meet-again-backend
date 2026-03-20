package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.matching.fixture.TargetInfoFixture
import com.konkuk.ma.domain.member.domain.Gender
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class TargetInfosTest : FunSpec({

    context("targetNames") {

        test("중복 없이 targetName 목록을 반환한다") {
            val targetInfos = TargetInfos(
                listOf(
                    TargetInfoFixture.create(targetInfoId = 1L, targetName = "홍길동"),
                    TargetInfoFixture.create(targetInfoId = 2L, targetName = "김철수"),
                    TargetInfoFixture.create(targetInfoId = 3L, targetName = "홍길동")
                )
            )

            targetInfos.targetNames() shouldContainExactlyInAnyOrder setOf("홍길동", "김철수")
        }

        test("빈 리스트이면 빈 Set을 반환한다") {
            val targetInfos = TargetInfos(emptyList())

            targetInfos.targetNames() shouldHaveSize 0
        }

        test("모든 targetName이 동일하면 하나만 반환한다") {
            val targetInfos = TargetInfos(
                listOf(
                    TargetInfoFixture.create(targetInfoId = 1L, targetName = "홍길동"),
                    TargetInfoFixture.create(targetInfoId = 2L, targetName = "홍길동")
                )
            )

            targetInfos.targetNames() shouldHaveSize 1
            targetInfos.targetNames().first() shouldBe "홍길동"
        }
    }

    context("makeMatchingResults") {

        test("모든 TargetInfo에 대해 매칭 결과를 생성하고 병합한다") {
            val targetInfos = TargetInfos(
                listOf(
                    TargetInfoFixture.create(
                        targetInfoId = 1L,
                        targetName = "홍길동",
                        targetGender = Gender.MALE
                    ),
                    TargetInfoFixture.create(
                        targetInfoId = 2L,
                        targetName = "김철수",
                        targetGender = Gender.MALE
                    )
                )
            )

            val targets = Targets.from(
                listOf(
                    MemberFixture.create(name = "홍길동", gender = Gender.MALE, email = "a@a.com"),
                    MemberFixture.create(name = "김철수", gender = Gender.MALE, email = "b@b.com")
                )
            )

            val results = targetInfos.makeMatchingResults(targets)

            results.data shouldHaveSize 2
        }

        test("매칭되는 Target이 없으면 빈 결과를 반환한다") {
            val targetInfos = TargetInfos(
                listOf(
                    TargetInfoFixture.create(targetName = "이영희", targetGender = Gender.FEMALE)
                )
            )

            val targets = Targets.from(
                listOf(
                    MemberFixture.create(name = "홍길동", gender = Gender.MALE)
                )
            )

            val results = targetInfos.makeMatchingResults(targets)

            results.data shouldHaveSize 0
        }

        test("빈 TargetInfos이면 빈 결과를 반환한다") {
            val targetInfos = TargetInfos(emptyList())

            val targets = Targets.from(
                listOf(MemberFixture.create())
            )

            val results = targetInfos.makeMatchingResults(targets)

            results.data shouldHaveSize 0
        }

        test("하나의 TargetInfo에 여러 Target이 매칭되면 모두 결과에 포함된다") {
            val targetInfos = TargetInfos(
                listOf(
                    TargetInfoFixture.create(targetName = "홍길동", targetGender = Gender.MALE)
                )
            )

            val targets = Targets.from(
                listOf(
                    MemberFixture.create(name = "홍길동", gender = Gender.MALE, email = "a@a.com"),
                    MemberFixture.create(name = "홍길동", gender = Gender.MALE, email = "b@b.com")
                )
            )

            val results = targetInfos.makeMatchingResults(targets)

            results.data shouldHaveSize 2
        }
    }
})
