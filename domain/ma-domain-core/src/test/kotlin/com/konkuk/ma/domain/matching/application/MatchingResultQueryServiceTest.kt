package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
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

    context("find") {

        test("매칭결과를 조회하고 회원정보와 사진정보를 조합하여 반환한다") {
            // Given
            val email = "register@example.com"
            val matchingResult = MatchingResultFixture.create(
                registerEmail = email,
                targetEmail = "target@example.com"
            )

            val member = MemberFixture.create(email = "target@example.com")
            val photo = MemberPhotoFixture.create(
                memberEmail = "target@example.com",
                thumbnailPath = "thumb/photo.jpg"
            )

            every { matchingResultRepository.find(any(), eq(false)) } returns listOf(matchingResult)
            every { memberQueryRepository.findByEmails(any()) } returns listOf(member)
            every { memberPhotoRepository.find(any()) } returns listOf(photo)

            // When
            val result = service.find(email)

            // Then
            result.data shouldHaveSize 1
            result.data[0].targetName shouldBe member.name
            result.data[0].targetNickname shouldBe member.nickname
            result.data[0].profileImageUrl shouldBe photo.thumbnailPath
        }

        test("excluded=true로 조회하면 제외된 매칭결과를 반환한다") {
            // Given
            val email = "register@example.com"
            val matchingResult = MatchingResultFixture.create(
                registerEmail = email,
                targetEmail = "target@example.com"
            )

            val member = MemberFixture.create(email = "target@example.com")
            val photo = MemberPhotoFixture.create(
                memberEmail = "target@example.com",
                thumbnailPath = "thumb/photo.jpg"
            )

            every { matchingResultRepository.find(any(), eq(true)) } returns listOf(matchingResult)
            every { memberQueryRepository.findByEmails(any()) } returns listOf(member)
            every { memberPhotoRepository.find(any()) } returns listOf(photo)

            // When
            val result = service.find(email, excluded = true)

            // Then
            result.data shouldHaveSize 1
            result.data[0].targetName shouldBe member.name
            result.data[0].targetNickname shouldBe member.nickname
            result.data[0].profileImageUrl shouldBe photo.thumbnailPath
        }

        test("매칭결과가 없으면 빈 결과를 반환한다") {
            // Given
            val email = "register@example.com"

            every { matchingResultRepository.find(any(), eq(false)) } returns emptyList()
            every { memberQueryRepository.findByEmails(any()) } returns emptyList()
            every { memberPhotoRepository.find(any()) } returns emptyList()

            // When
            val result = service.find(email)

            // Then
            result.data shouldHaveSize 0
        }
    }

    context("findDetail") {

        test("정상 조회 시 MatchingResult를 반환한다") {
            // Given
            val matchingResultId = 1L
            val email = "register@example.com"
            val matchingResult = MatchingResultFixture.create(
                registerEmail = email,
                targetEmail = "target@example.com"
            )

            every { matchingResultRepository.findOne(matchingResultId) } returns matchingResult

            // When
            val result = service.findDetail(matchingResultId, email)

            // Then
            result shouldBe matchingResult
        }

        test("존재하지 않는 ID이면 EntityNotFoundException이 발생한다") {
            // Given
            val nonExistentId = 999L
            val email = "register@example.com"

            every { matchingResultRepository.findOne(nonExistentId) } throws EntityNotFoundException(EntityType.MATCHING_RESULT, nonExistentId.toString())

            // When & Then
            shouldThrow<EntityNotFoundException> {
                service.findDetail(nonExistentId, email)
            }
        }

        test("소유권이 없는 경우 AccessDeniedException이 발생한다") {
            // Given
            val matchingResultId = 1L
            val ownerEmail = "owner@example.com"
            val otherEmail = "other@example.com"
            val matchingResult = MatchingResultFixture.create(registerEmail = ownerEmail)

            every { matchingResultRepository.findOne(matchingResultId) } returns matchingResult

            // When & Then
            shouldThrow<AccessDeniedException> {
                service.findDetail(matchingResultId, otherEmail)
            }
        }
    }
})
