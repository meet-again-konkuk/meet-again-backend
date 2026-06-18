package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MembersTest : FunSpec({

    context("findOne") {

        test("이메일로 회원을 찾으면 해당 회원을 반환한다") {
            // Given
            val member = MemberFixture.create(email = "target@example.com")
            val members = Members(listOf(member))

            // When
            val result = members.findOne(member.email)

            // Then
            result shouldBe member
        }

        test("여러 회원 중 이메일로 정확한 회원을 찾는다") {
            // Given
            val member1 = MemberFixture.create(email = "a@example.com", nickname = "닉네임A")
            val member2 = MemberFixture.create(email = "b@example.com", nickname = "닉네임B")
            val members = Members(listOf(member1, member2))

            // When
            val result = members.findOne(member2.email)

            // Then
            result shouldBe member2
        }

        test("존재하지 않는 이메일이면 null을 반환한다") {
            // Given
            val member = MemberFixture.create(email = "existing@example.com")
            val members = Members(listOf(member))

            // When
            val result = members.findOne(Email("notfound@example.com"))

            // Then
            result shouldBe null
        }

        test("빈 목록에서 조회하면 null을 반환한다") {
            // Given
            val members = Members(emptyList())

            // When
            val result = members.findOne(Email("any@example.com"))

            // Then
            result shouldBe null
        }
    }

    context("findOne(id)") {

        test("id로 회원을 찾으면 해당 회원을 반환한다") {
            // Given
            val member = MemberFixture.create(id = 1L)
            val members = Members(listOf(member))

            // When
            val result = members.findOne(member.id)

            // Then
            result shouldBe member
        }

        test("여러 회원 중 id로 정확한 회원을 찾는다") {
            // Given
            val member1 = MemberFixture.create(id = 1L, email = "a@example.com")
            val member2 = MemberFixture.create(id = 2L, email = "b@example.com")
            val members = Members(listOf(member1, member2))

            // When
            val result = members.findOne(member2.id)

            // Then
            result shouldBe member2
        }

        test("존재하지 않는 id이면 null을 반환한다") {
            // Given
            val member = MemberFixture.create(id = 1L)
            val members = Members(listOf(member))

            // When
            val result = members.findOne(999L)

            // Then
            result shouldBe null
        }

        test("빈 목록에서 조회하면 null을 반환한다") {
            // Given
            val members = Members(emptyList())

            // When
            val result = members.findOne(1L)

            // Then
            result shouldBe null
        }
    }

    context("findNickname") {

        test("이메일로 닉네임을 찾으면 해당 닉네임을 반환한다") {
            // Given
            val member = MemberFixture.create(email = "user@example.com", nickname = "테스트닉네임")
            val members = Members(listOf(member))

            // When
            val result = members.findNickname(member.email)

            // Then
            result shouldBe member.nickname
        }

        test("여러 회원 중 정확한 이메일의 닉네임을 반환한다") {
            // Given
            val member1 = MemberFixture.create(email = "a@example.com", nickname = "닉네임A")
            val member2 = MemberFixture.create(email = "b@example.com", nickname = "닉네임B")
            val members = Members(listOf(member1, member2))

            // When
            val result = members.findNickname(member2.email)

            // Then
            result shouldBe member2.nickname
        }

        test("존재하지 않는 이메일이면 '알 수 없음'을 반환한다") {
            // Given
            val member = MemberFixture.create(email = "existing@example.com")
            val members = Members(listOf(member))

            // When
            val result = members.findNickname(Email("notfound@example.com"))

            // Then
            result shouldBe "알 수 없음"
        }

        test("빈 목록에서 조회하면 '알 수 없음'을 반환한다") {
            // Given
            val members = Members(emptyList())

            // When
            val result = members.findNickname(Email("any@example.com"))

            // Then
            result shouldBe "알 수 없음"
        }
    }
})
