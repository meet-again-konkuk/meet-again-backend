package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.MatchingResults
import com.konkuk.ma.domain.matching.exception.MatchingResultAccessDeniedException
import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.photo.MemberPhotos
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.member.fixture.MemberPhotoFixture
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

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

    context("findDetailById") {

        test("정상 조회 시 MatchingResult를 반환한다") {
            // Given
            val matchingResultId = 1L
            val email = "register@example.com"
            val matchingResult = MatchingResultFixture.create(
                registerEmail = email,
                targetEmail = "target@example.com"
            )

            every { matchingResultRepository.findById(matchingResultId) } returns matchingResult

            // When
            val result = service.findDetailById(matchingResultId, email)

            // Then
            result shouldBe matchingResult
        }

        test("존재하지 않는 ID이면 EntityNotFoundException이 발생한다") {
            // Given
            val nonExistentId = 999L
            val email = "register@example.com"

            every { matchingResultRepository.findById(nonExistentId) } throws EntityNotFoundException(EntityType.MATCHING_RESULT, nonExistentId.toString())

            // When & Then
            shouldThrow<EntityNotFoundException> {
                service.findDetailById(nonExistentId, email)
            }.message shouldBe "MatchingResult을(를) 찾을 수 없습니다."
        }

        test("소유권이 없는 경우 MatchingResultAccessDeniedException이 발생한다") {
            // Given
            val matchingResultId = 1L
            val ownerEmail = "owner@example.com"
            val otherEmail = "other@example.com"
            val matchingResult = MatchingResultFixture.create(registerEmail = ownerEmail)

            every { matchingResultRepository.findById(matchingResultId) } returns matchingResult

            // When & Then
            shouldThrow<MatchingResultAccessDeniedException> {
                service.findDetailById(matchingResultId, otherEmail)
            }.message shouldBe "매칭 결과에 대한 접근 권한이 없습니다."
        }
    }
})
