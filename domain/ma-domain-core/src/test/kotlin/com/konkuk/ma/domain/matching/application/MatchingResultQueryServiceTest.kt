package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.common.domain.file.port.FileUrlResolver
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoUrlResolver
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.member.fixture.MemberPhotoFixture
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
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
    val fileUrlResolver = mockk<FileUrlResolver>()
    val memberPhotoUrlResolver = MemberPhotoUrlResolver(fileUrlResolver)
    val xroomQueryRepository = mockk<XroomQueryRepository>()
    val service = MatchingResultQueryService(
        matchingResultRepository,
        memberQueryRepository,
        memberPhotoRepository,
        memberPhotoUrlResolver,
        xroomQueryRepository,
    )

    beforeEach {
        clearAllMocks()
        every { fileUrlResolver.resolve(any()) } answers { "/files/${firstArg<String>()}" }
    }

    context("find") {

        test("매칭결과를 조회하고 회원정보와 사진정보를 조합하여 반환한다") {
            // Given
            val memberId = 1L
            val matchingResult = MatchingResultFixture.create(
                registerId = memberId,
                targetId = 2L
            )

            val member = MemberFixture.create(id = 2L, email = "target@example.com")
            val photo = MemberPhotoFixture.create(
                memberId = member.id,
                thumbnailKey = "member/thumbnail/${member.id}/thumb_photo.jpg"
            )

            every { matchingResultRepository.find(any(), eq(false)) } returns listOf(matchingResult)
            every { memberQueryRepository.findByIds(any()) } returns listOf(member)
            every { memberPhotoRepository.find(any()) } returns listOf(photo)

            // When
            val result = service.find(memberId)

            // Then
            result.data shouldHaveSize 1
            result.data[0].targetName shouldBe member.name
            result.data[0].targetNickname shouldBe member.nickname
            result.data[0].profileImageUrl shouldBe "/files/${photo.thumbnailKey}"
        }

        test("excluded=true로 조회하면 제외된 매칭결과를 반환한다") {
            // Given
            val memberId = 1L
            val matchingResult = MatchingResultFixture.create(
                registerId = memberId,
                targetId = 2L
            )

            val member = MemberFixture.create(id = 2L, email = "target@example.com")
            val photo = MemberPhotoFixture.create(
                memberId = member.id,
                thumbnailKey = "member/thumbnail/${member.id}/thumb_photo.jpg"
            )

            every { matchingResultRepository.find(any(), eq(true)) } returns listOf(matchingResult)
            every { memberQueryRepository.findByIds(any()) } returns listOf(member)
            every { memberPhotoRepository.find(any()) } returns listOf(photo)

            // When
            val result = service.find(memberId, excluded = true)

            // Then
            result.data shouldHaveSize 1
            result.data[0].targetName shouldBe member.name
            result.data[0].targetNickname shouldBe member.nickname
            result.data[0].profileImageUrl shouldBe "/files/${photo.thumbnailKey}"
        }

        test("매칭결과가 없으면 빈 결과를 반환한다") {
            // Given
            val memberId = 1L

            every { matchingResultRepository.find(any(), eq(false)) } returns emptyList()
            every { memberQueryRepository.findByIds(any()) } returns emptyList()
            every { memberPhotoRepository.find(any()) } returns emptyList()

            // When
            val result = service.find(memberId)

            // Then
            result.data shouldHaveSize 0
        }
    }

    context("findDetail") {

        test("정상 조회 시 MatchingResult를 반환한다") {
            // Given
            val matchingResultId = 1L
            val memberId = 1L
            val matchingResult = MatchingResultFixture.create(
                registerId = memberId,
                targetId = 2L
            )

            every { matchingResultRepository.findOne(matchingResultId) } returns matchingResult

            // When
            val result = service.findDetail(matchingResultId, memberId)

            // Then
            result shouldBe matchingResult
        }

        test("존재하지 않는 ID이면 EntityNotFoundException이 발생한다") {
            // Given
            val nonExistentId = 999L
            val memberId = 1L

            every { matchingResultRepository.findOne(nonExistentId) } throws EntityNotFoundException(EntityType.MATCHING_RESULT, nonExistentId.toString())

            // When & Then
            shouldThrow<EntityNotFoundException> {
                service.findDetail(nonExistentId, memberId)
            }
        }

        test("소유권이 없는 경우 AccessDeniedException이 발생한다") {
            // Given
            val matchingResultId = 1L
            val ownerId = 1L
            val otherId = 2L
            val matchingResult = MatchingResultFixture.create(registerId = ownerId)

            every { matchingResultRepository.findOne(matchingResultId) } returns matchingResult

            // When & Then
            shouldThrow<AccessDeniedException> {
                service.findDetail(matchingResultId, otherId)
            }
        }
    }

    context("findClaimedBy") {

        test("요청자 프로필과 X룸 존재 여부를 조합하여 반환한다") {
            val myId = 1L
            val claimer1 = MatchingResultFixture.create(
                targetInfoId = 10L,
                registerId = 100L,
                targetId = myId,
            )
            val claimer2 = MatchingResultFixture.create(
                targetInfoId = 20L,
                registerId = 200L,
                targetId = myId,
            )
            val member1 = MemberFixture.create(id = 100L, email = "claimer1@a.com", name = "갑")
            val member2 = MemberFixture.create(id = 200L, email = "claimer2@a.com", name = "을")
            val photo1 = MemberPhotoFixture.create(memberId = member1.id, thumbnailKey = "member/thumbnail/${member1.id}/thumb_1.jpg")

            every { matchingResultRepository.findClaimedByTarget(myId) } returns listOf(claimer1, claimer2)
            every { memberQueryRepository.findByIds(any()) } returns listOf(member1, member2)
            every { memberPhotoRepository.find(any()) } returns listOf(photo1)
            every { xroomQueryRepository.exists(setOf(10L, 20L)) } returns setOf(10L)

            val profiles = service.findClaimedBy(myId)

            profiles.data shouldHaveSize 2
            profiles.data[0].name shouldBe "갑"
            profiles.data[0].profileImageUrl shouldBe "/files/${photo1.thumbnailKey}"
            profiles.data[0].hasXroom shouldBe true
            profiles.data[1].name shouldBe "을"
            profiles.data[1].hasXroom shouldBe false
        }

        test("claim 목록이 비어있으면 빈 프로필을 반환한다") {
            val myId = 1L

            every { matchingResultRepository.findClaimedByTarget(myId) } returns emptyList()
            every { memberQueryRepository.findByIds(any()) } returns emptyList()
            every { memberPhotoRepository.find(any()) } returns emptyList()
            every { xroomQueryRepository.exists(emptySet<Long>()) } returns emptySet()

            val profiles = service.findClaimedBy(myId)

            profiles.data shouldHaveSize 0
        }
    }
})
