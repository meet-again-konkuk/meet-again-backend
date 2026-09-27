package com.konkuk.ma.domain.member.api.response

import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.MemberProfile
import com.konkuk.ma.domain.member.domain.Region
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate

/**
 * 내 프로필 응답 매핑 계약 테스트.
 *
 * GET · PATCH 가 같은 11필드를 내려주므로, 이 매핑이 곧 두 엔드포인트의 응답 계약이다.
 * enum 은 displayName 이 아니라 **코드**를, 전화번호는 하이픈 없는 fullNumber 를 담는다.
 */
class MyProfileResponseTest : FunSpec({

    context("from") {

        test("회원과 프로필 이미지 URL 을 11개 응답 필드로 매핑한다") {
            // Given
            val member = MemberFixture.create(
                id = 42L,
                email = "profile@example.com",
                nickname = "응원단장",
                name = "홍길동",
                gender = Gender.FEMALE,
                phoneNumber = "01012345678",
                region = Region.BUSAN,
                birthDate = LocalDate.of(1995, 3, 7),
                highSchool = "건대부고",
                university = "건국대",
            )
            val profile = MemberProfile.of(member, "/files/member/thumbnail/42/thumb_photo.jpg")

            // When
            val response = MyProfileResponse.from(profile)

            // Then
            response.memberId shouldBe member.id
            response.email shouldBe member.email.value
            response.nickname shouldBe member.nickname
            response.name shouldBe member.name
            response.gender shouldBe member.gender
            response.birthDate shouldBe member.birthDate
            response.phoneNumber shouldBe member.phoneNumber.fullNumber
            response.region shouldBe member.region
            response.highSchool shouldBe member.highSchool
            response.university shouldBe member.university
            response.profileImageUrl shouldBe profile.profileImageUrl
        }

        test("전화번호는 하이픈 없는 fullNumber 형식으로 담는다") {
            // Given
            val member = MemberFixture.create(phoneNumber = "010-1234-5678")
            val profile = MemberProfile.of(member, null)

            // When
            val response = MyProfileResponse.from(profile)

            // Then
            response.phoneNumber shouldBe "01012345678"
        }

        test("공백이 섞인 전화번호도 하이픈·공백 없이 담는다") {
            // Given
            val member = MemberFixture.create(phoneNumber = "010 1234 5678")
            val profile = MemberProfile.of(member, null)

            // When
            val response = MyProfileResponse.from(profile)

            // Then
            response.phoneNumber shouldBe "01012345678"
        }

        test("프로필 사진이 없으면 profileImageUrl 이 null 이다") {
            // Given
            val profile = MemberProfile.of(MemberFixture.create(), null)

            // When
            val response = MyProfileResponse.from(profile)

            // Then
            response.profileImageUrl.shouldBeNull()
        }

        test("고등학교와 대학교가 없으면 null 을 그대로 담는다") {
            // Given
            val member = MemberFixture.create(highSchool = null, university = null)
            val profile = MemberProfile.of(member, null)

            // When
            val response = MyProfileResponse.from(profile)

            // Then
            response.highSchool.shouldBeNull()
            response.university.shouldBeNull()
        }

        test("고등학교만 있고 대학교가 없는 회원도 매핑한다") {
            // Given
            val member = MemberFixture.create(highSchool = "건대부고", university = null)
            val profile = MemberProfile.of(member, null)

            // When
            val response = MyProfileResponse.from(profile)

            // Then
            response.highSchool shouldBe member.highSchool
            response.university.shouldBeNull()
        }

        test("성별은 displayName 이 아니라 enum 상수를 담는다") {
            // Given
            val member = MemberFixture.create(gender = Gender.MALE)
            val profile = MemberProfile.of(member, null)

            // When
            val response = MyProfileResponse.from(profile)

            // Then
            response.gender shouldBe Gender.MALE
        }

        test("지역은 displayName(\"제주도\") 이 아니라 enum 상수를 담는다") {
            // Given
            val member = MemberFixture.create(region = Region.JEJU_DO)
            val profile = MemberProfile.of(member, null)

            // When
            val response = MyProfileResponse.from(profile)

            // Then
            response.region shouldBe Region.JEJU_DO
        }

        test("memberId 는 도메인 회원의 원시 id 를 담는다 (인코딩은 직렬화 시점)") {
            // Given
            val member = MemberFixture.create(id = 7L)
            val profile = MemberProfile.of(member, null)

            // When
            val response = MyProfileResponse.from(profile)

            // Then
            response.memberId shouldBe 7L
        }

        test("고등학교가 빈 문자열이면 빈 문자열 그대로 담는다") {
            // Given — 빈 문자열은 null 로 바꾸지 않는다
            val member = MemberFixture.create(highSchool = "")
            val profile = MemberProfile.of(member, null)

            // When
            val response = MyProfileResponse.from(profile)

            // Then
            response.highSchool shouldBe ""
        }
    }
})
