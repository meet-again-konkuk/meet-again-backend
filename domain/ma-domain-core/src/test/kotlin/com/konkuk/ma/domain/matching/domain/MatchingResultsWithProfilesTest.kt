package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.fixture.MemberPhotoFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class MatchingResultsWithProfilesTest : FunSpec({

    context("combine") {

        test("매칭결과와 회원정보와 사진정보를 조합하여 프로필을 생성한다") {
            // Given
            val matchingResult = MatchingResultFixture.create(targetEmail = "target@example.com")
            val matchingResults = MatchingResults(listOf(matchingResult))

            val member = MemberFixture.create(email = matchingResult.targetEmail, name = "홍길동", nickname = "닉네임")
            val membersByEmail = mapOf(member.email to member)

            val photo = MemberPhotoFixture.create(
                memberEmail = matchingResult.targetEmail,
                thumbnailPath = "thumb/photo.jpg"
            )
            val photosByEmail = mapOf(matchingResult.targetEmail to photo)

            // When
            val result = MatchingResultsWithProfiles.combine(matchingResults, membersByEmail, photosByEmail)

            // Then
            result.data shouldHaveSize 1
            result.data[0].matchingResult shouldBe matchingResult
            result.data[0].targetName shouldBe member.name
            result.data[0].targetNickname shouldBe member.nickname
            result.data[0].profileImageUrl shouldBe photo.thumbnailPath
        }

        test("탈퇴한 회원은 결과에서 제외된다") {
            // Given
            val activeResult = MatchingResultFixture.create(targetEmail = "active@example.com")
            val withdrawnResult = MatchingResultFixture.create(targetEmail = "withdrawn@example.com")
            val matchingResults = MatchingResults(listOf(activeResult, withdrawnResult))

            val activeMember = MemberFixture.create(email = activeResult.targetEmail)
            val membersByEmail = mapOf(activeMember.email to activeMember)

            val photosByEmail = emptyMap<String, com.konkuk.ma.domain.member.domain.photo.MemberPhoto>()

            // When
            val result = MatchingResultsWithProfiles.combine(matchingResults, membersByEmail, photosByEmail)

            // Then
            result.data shouldHaveSize 1
            result.data[0].matchingResult shouldBe activeResult
        }

        test("사진이 없는 회원은 profileImageUrl이 null이다") {
            // Given
            val matchingResult = MatchingResultFixture.create(targetEmail = "target@example.com")
            val matchingResults = MatchingResults(listOf(matchingResult))

            val member = MemberFixture.create(email = matchingResult.targetEmail)
            val membersByEmail = mapOf(member.email to member)

            val photosByEmail = emptyMap<String, com.konkuk.ma.domain.member.domain.photo.MemberPhoto>()

            // When
            val result = MatchingResultsWithProfiles.combine(matchingResults, membersByEmail, photosByEmail)

            // Then
            result.data shouldHaveSize 1
            result.data[0].profileImageUrl shouldBe null
        }

        test("빈 매칭결과이면 빈 프로필 목록을 반환한다") {
            // Given
            val matchingResults = MatchingResults(emptyList())
            val membersByEmail = emptyMap<String, com.konkuk.ma.domain.member.domain.Member>()
            val photosByEmail = emptyMap<String, com.konkuk.ma.domain.member.domain.photo.MemberPhoto>()

            // When
            val result = MatchingResultsWithProfiles.combine(matchingResults, membersByEmail, photosByEmail)

            // Then
            result.data shouldHaveSize 0
        }

        test("여러 매칭결과를 한번에 조합한다") {
            // Given
            val result1 = MatchingResultFixture.create(targetEmail = "a@example.com", targetInfoId = 1L)
            val result2 = MatchingResultFixture.create(targetEmail = "b@example.com", targetInfoId = 2L)
            val matchingResults = MatchingResults(listOf(result1, result2))

            val memberA = MemberFixture.create(email = result1.targetEmail, name = "김철수", nickname = "철수")
            val memberB = MemberFixture.create(email = result2.targetEmail, name = "이영희", nickname = "영희")
            val membersByEmail = mapOf(memberA.email to memberA, memberB.email to memberB)

            val photoA = MemberPhotoFixture.create(memberEmail = result1.targetEmail, thumbnailPath = "thumb/a.jpg")
            val photosByEmail = mapOf(result1.targetEmail to photoA)

            // When
            val result = MatchingResultsWithProfiles.combine(matchingResults, membersByEmail, photosByEmail)

            // Then
            result.data shouldHaveSize 2
            result.data[0].targetName shouldBe memberA.name
            result.data[0].profileImageUrl shouldBe photoA.thumbnailPath
            result.data[1].targetName shouldBe memberB.name
            result.data[1].profileImageUrl shouldBe null
        }
    }
})
