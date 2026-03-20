package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.Gender
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class TargetsTest : FunSpec({

    context("filterCandidates") {

        test("이름과 성별이 모두 일치하는 Target만 반환한다") {
            val targets = Targets.from(
                listOf(
                    MemberFixture.create(name = "홍길동", gender = Gender.MALE, email = "a@a.com"),
                    MemberFixture.create(name = "홍길동", gender = Gender.FEMALE, email = "b@b.com"),
                    MemberFixture.create(name = "김철수", gender = Gender.MALE, email = "c@c.com")
                )
            )

            val candidates = targets.filterCandidates("홍길동", Gender.MALE)

            candidates shouldHaveSize 1
            candidates.first().email shouldBe "a@a.com"
        }

        test("이름만 일치하고 성별이 다르면 반환하지 않는다") {
            val targets = Targets.from(
                listOf(
                    MemberFixture.create(name = "홍길동", gender = Gender.FEMALE, email = "a@a.com")
                )
            )

            val candidates = targets.filterCandidates("홍길동", Gender.MALE)

            candidates shouldHaveSize 0
        }

        test("일치하는 Target이 여러 명이면 모두 반환한다") {
            val targets = Targets.from(
                listOf(
                    MemberFixture.create(name = "홍길동", gender = Gender.MALE, email = "a@a.com"),
                    MemberFixture.create(name = "홍길동", gender = Gender.MALE, email = "b@b.com")
                )
            )

            val candidates = targets.filterCandidates("홍길동", Gender.MALE)

            candidates shouldHaveSize 2
        }

        test("빈 Targets에서 필터링하면 빈 리스트를 반환한다") {
            val targets = Targets(emptyList())

            val candidates = targets.filterCandidates("홍길동", Gender.MALE)

            candidates shouldHaveSize 0
        }
    }

    context("from") {

        test("Member 리스트로부터 Targets를 생성한다") {
            val members = listOf(
                MemberFixture.create(email = "a@a.com", name = "홍길동"),
                MemberFixture.create(email = "b@b.com", name = "김철수")
            )

            val targets = Targets.from(members)

            targets.data shouldHaveSize 2
            targets.data[0].email shouldBe "a@a.com"
            targets.data[0].name shouldBe "홍길동"
            targets.data[1].email shouldBe "b@b.com"
            targets.data[1].name shouldBe "김철수"
        }

        test("빈 Member 리스트로부터 빈 Targets를 생성한다") {
            val targets = Targets.from(emptyList())

            targets.data shouldHaveSize 0
        }
    }
})
