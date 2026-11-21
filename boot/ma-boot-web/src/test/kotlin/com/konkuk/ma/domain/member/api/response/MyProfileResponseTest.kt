package com.konkuk.ma.domain.member.api.response

import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.MemberProfile
import com.konkuk.ma.domain.member.domain.Region
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class MyProfileResponseTest : FunSpec({

    context("from") {

        test("회원과 프로필 이미지 URL 을 11개 응답 필드로 매핑한다") {
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

            val response = MyProfileResponse.from(profile)

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
            val member = MemberFixture.create(phoneNumber = "010-1234-5678")
            val profile = MemberProfile.of(member, null)

            val response = MyProfileResponse.from(profile)

            response.phoneNumber shouldBe "01012345678"
        }

        test("공백이 섞인 전화번호도 하이픈·공백 없이 담는다") {
            val member = MemberFixture.create(phoneNumber = "010 1234 5678")
            val profile = MemberProfile.of(member, null)

            val response = MyProfileResponse.from(profile)

            response.phoneNumber shouldBe "01012345678"
        }

        test("프로필 사진이 없으면 profileImageUrl 이 null 이다") {
            val profile = MemberProfile.of(MemberFixture.create(), null)

            val response = MyProfileResponse.from(profile)

            response.profileImageUrl.shouldBeNull()
        }

        test("고등학교와 대학교가 없으면 null 을 그대로 담는다") {
            val member = MemberFixture.create(highSchool = null, university = null)
            val profile = MemberProfile.of(member, null)

            val response = MyProfileResponse.from(profile)

            response.highSchool.shouldBeNull()
            response.university.shouldBeNull()
        }

        test("고등학교만 있고 대학교가 없는 회원도 매핑한다") {
            val member = MemberFixture.create(highSchool = "건대부고", university = null)
            val profile = MemberProfile.of(member, null)

            val response = MyProfileResponse.from(profile)

            response.highSchool shouldBe member.highSchool
            response.university.shouldBeNull()
        }

        test("성별은 displayName 이 아니라 enum 상수를 담는다") {
            val member = MemberFixture.create(gender = Gender.MALE)
            val profile = MemberProfile.of(member, null)

            val response = MyProfileResponse.from(profile)

            response.gender shouldBe Gender.MALE
        }

        test("지역은 displayName(\"제주도\") 이 아니라 enum 상수를 담는다") {
            val member = MemberFixture.create(region = Region.JEJU_DO)
            val profile = MemberProfile.of(member, null)

            val response = MyProfileResponse.from(profile)

            response.region shouldBe Region.JEJU_DO
        }

        test("memberId 는 도메인 회원의 원시 id 를 담는다 (인코딩은 직렬화 시점)") {
            val member = MemberFixture.create(id = 7L)
            val profile = MemberProfile.of(member, null)

            val response = MyProfileResponse.from(profile)

            response.memberId shouldBe 7L
        }

        test("고등학교가 빈 문자열이면 빈 문자열 그대로 담는다") {
            val member = MemberFixture.create(highSchool = "")
            val profile = MemberProfile.of(member, null)

            val response = MyProfileResponse.from(profile)

            response.highSchool shouldBe ""
        }
    }
})
