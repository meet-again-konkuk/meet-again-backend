package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.MatchingResults
import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.photo.MemberPhotos
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.member.fixture.MemberPhotoFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class MatchingResultQueryServiceTest : FunSpec({

    val matchingResultRepository = mockk<MatchingResultRepository>()
    val memberQueryRepository = mockk<MemberQueryRepository>()
    val memberPhotoRepository = mockk<MemberPhotoRepository>()
    val service = MatchingResultQueryService(matchingResultRepository, memberQueryRepository, memberPhotoRepository)

    beforeEach {
        clearAllMocks()
    }

    context("findByRegisterEmail") {

        test("매칭결과를 조회하고 회원정보와 사진정보를 조합하여 반환한다") {
            // Given
            val email = "register@example.com"
            val matchingResult = MatchingResultFixture.create(
                registerEmail = email,
                targetEmail = "target@example.com"
            )
            val matchingResults = MatchingResults(listOf(matchingResult))

            val member = MemberFixture.create(email = matchingResult.targetEmail)
            val photo = MemberPhotoFixture.create(
                memberEmail = matchingResult.targetEmail,
                thumbnailPath = "thumb/photo.jpg"
            )

            every { matchingResultRepository.findByRegisterEmail(email) } returns matchingResults
            every { memberQueryRepository.findByEmails(matchingResults.extractTargetEmails()) } returns Members(listOf(member))
            every { memberPhotoRepository.findByEmails(matchingResults.extractTargetEmails()) } returns MemberPhotos(listOf(photo))

            // When
            val result = service.findByRegisterEmail(email)

            // Then
            result.data shouldHaveSize 1
            result.data[0].targetName shouldBe member.name
            result.data[0].targetNickname shouldBe member.nickname
            result.data[0].profileImageUrl shouldBe photo.thumbnailPath
        }

        test("매칭결과가 없으면 빈 결과를 반환한다") {
            // Given
            val email = "register@example.com"
            val emptyResults = MatchingResults(emptyList())
            val emptyEmails = emptyResults.extractTargetEmails()

            every { matchingResultRepository.findByRegisterEmail(email) } returns emptyResults
            every { memberQueryRepository.findByEmails(emptyEmails) } returns Members(emptyList())
            every { memberPhotoRepository.findByEmails(emptyEmails) } returns MemberPhotos(emptyList())

            // When
            val result = service.findByRegisterEmail(email)

            // Then
            result.data shouldHaveSize 0
        }
    }
})
