package com.konkuk.ma.job.domain.matching

import com.konkuk.ma.domain.matching.domain.TargetInfos
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.matching.fixture.TargetInfoFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class MatchingProcessorTest : FunSpec({

    val targetInfoQueryRepository = mockk<TargetInfoQueryRepository>()
    val memberQueryRepository = mockk<MemberQueryRepository>()
    val matchingResultRepository = mockk<MatchingResultRepository>()

    val config = MatchingJobConfig(targetInfoQueryRepository, memberQueryRepository, matchingResultRepository)
    val processor = config.matchingProcessor()

    context("matchingProcessor") {

        test("TargetInfo와 매칭되는 Member가 있으면 MatchingResult를 생성한다") {
            // Given
            val targetInfo = TargetInfoFixture.create()
            val member = MemberFixture.create(name = targetInfo.targetName)
            val targetInfos = TargetInfos(listOf(targetInfo))

            every { memberQueryRepository.findByNames(targetInfos.extractTargetNames()) } returns listOf(member)
            every { matchingResultRepository.findExistingMatchingResults(listOf(targetInfo.targetInfoId)) } returns emptyList()

            // When
            val result = processor.process(targetInfos.data)

            // Then
            result!! shouldHaveSize 1
            result[0].targetInfoId shouldBe targetInfo.targetInfoId
            result[0].targetEmail shouldBe member.email
        }

        test("매칭되는 Member가 없으면 빈 결과를 반환한다") {
            // Given
            val targetInfo = TargetInfoFixture.create()
            val targetInfos = TargetInfos(listOf(targetInfo))

            every { memberQueryRepository.findByNames(targetInfos.extractTargetNames()) } returns emptyList()
            every { matchingResultRepository.findExistingMatchingResults(emptyList()) } returns emptyList()

            // When
            val result = processor.process(targetInfos.data)

            // Then
            result!! shouldHaveSize 0
        }

        test("이름은 같지만 target 성별이 다르면 매칭되지 않는다") {
            // Given
            val targetInfo = TargetInfoFixture.create(targetGender = Gender.MALE)
            val member = MemberFixture.create(name = targetInfo.targetName, gender = Gender.FEMALE)
            val targetInfos = TargetInfos(listOf(targetInfo))

            every { memberQueryRepository.findByNames(targetInfos.extractTargetNames()) } returns listOf(member)
            every { matchingResultRepository.findExistingMatchingResults(emptyList()) } returns emptyList()

            // When
            val result = processor.process(targetInfos.data)

            // Then
            result!! shouldHaveSize 0
        }

        test("이름이 다르면 매칭되지 않는다") {
            // Given
            val targetInfo = TargetInfoFixture.create(targetName = "홍길동")
            val member = MemberFixture.create(name = "김철수", gender = targetInfo.targetGender)
            val targetInfos = TargetInfos(listOf(targetInfo))

            every { memberQueryRepository.findByNames(targetInfos.extractTargetNames()) } returns listOf(member)
            every { matchingResultRepository.findExistingMatchingResults(emptyList()) } returns emptyList()

            // When
            val result = processor.process(targetInfos.data)

            // Then
            result!! shouldHaveSize 0
        }

        test("이미 존재하는 매칭 결과는 필터링된다") {
            // Given
            val targetInfo = TargetInfoFixture.create()
            val member = MemberFixture.create(name = targetInfo.targetName)
            val existingResult = MatchingResultFixture.create(
                targetInfoId = targetInfo.targetInfoId,
                targetEmail = member.email
            )
            val targetInfos = TargetInfos(listOf(targetInfo))

            every { memberQueryRepository.findByNames(targetInfos.extractTargetNames()) } returns listOf(member)
            every { matchingResultRepository.findExistingMatchingResults(listOf(targetInfo.targetInfoId)) } returns listOf(existingResult)

            // When
            val result = processor.process(targetInfos.data)

            // Then
            result!! shouldHaveSize 0
        }

        test("여러 매칭 결과 중 일부만 기존에 존재하면 새로운 결과만 반환한다") {
            // Given
            val targetInfo = TargetInfoFixture.create()
            val member1 = MemberFixture.create(name = targetInfo.targetName, email = "member1@example.com")
            val member2 = MemberFixture.create(name = targetInfo.targetName, email = "member2@example.com")
            val existingResult = MatchingResultFixture.create(
                targetInfoId = targetInfo.targetInfoId,
                targetEmail = member1.email
            )
            val targetInfos = TargetInfos(listOf(targetInfo))

            every { memberQueryRepository.findByNames(targetInfos.extractTargetNames()) } returns listOf(member1, member2)
            every { matchingResultRepository.findExistingMatchingResults(listOf(targetInfo.targetInfoId)) } returns listOf(existingResult)

            // When
            val result = processor.process(targetInfos.data)

            // Then
            result!! shouldHaveSize 1
            result[0].targetInfoId shouldBe targetInfo.targetInfoId
            result[0].targetEmail shouldBe member2.email
        }

        test("여러 TargetInfo에 대해 각각 매칭 결과를 생성한다") {
            // Given
            val targetInfo1 = TargetInfoFixture.create(targetInfoId = 1L, targetName = "홍길동", targetGender = Gender.MALE)
            val targetInfo2 = TargetInfoFixture.create(targetInfoId = 2L, targetName = "김영희", targetGender = Gender.FEMALE)
            val member1 = MemberFixture.create(name = targetInfo1.targetName, gender = targetInfo1.targetGender, email = "hong@example.com")
            val member2 = MemberFixture.create(name = targetInfo2.targetName, gender = targetInfo2.targetGender, email = "kim@example.com")
            val targetInfos = TargetInfos(listOf(targetInfo1, targetInfo2))

            every { memberQueryRepository.findByNames(targetInfos.extractTargetNames()) } returns listOf(member1, member2)
            every { matchingResultRepository.findExistingMatchingResults(listOf(targetInfo1.targetInfoId, targetInfo2.targetInfoId)) } returns emptyList()

            // When
            val result = processor.process(targetInfos.data)

            // Then
            result!! shouldHaveSize 2
            result[0].targetInfoId shouldBe targetInfo1.targetInfoId
            result[0].targetEmail shouldBe member1.email
            result[1].targetInfoId shouldBe targetInfo2.targetInfoId
            result[1].targetEmail shouldBe member2.email
        }

        test("여러 TargetInfo 중 일부만 매칭되는 Member가 있으면 매칭된 것만 결과에 포함된다") {
            // Given
            val targetInfo1 = TargetInfoFixture.create(targetInfoId = 1L, targetName = "홍길동", targetGender = Gender.MALE)
            val targetInfo2 = TargetInfoFixture.create(targetInfoId = 2L, targetName = "김영희", targetGender = Gender.FEMALE)
            val member1 = MemberFixture.create(name = targetInfo1.targetName, gender = targetInfo1.targetGender, email = "hong@example.com")
            val targetInfos = TargetInfos(listOf(targetInfo1, targetInfo2))

            every { memberQueryRepository.findByNames(targetInfos.extractTargetNames()) } returns listOf(member1)
            every { matchingResultRepository.findExistingMatchingResults(listOf(targetInfo1.targetInfoId)) } returns emptyList()

            // When
            val result = processor.process(targetInfos.data)

            // Then
            result!! shouldHaveSize 1
            result[0].targetInfoId shouldBe targetInfo1.targetInfoId
            result[0].targetEmail shouldBe member1.email
        }

        test("하나의 TargetInfo에 여러 Member가 매칭되면 모두 포함한다") {
            // Given
            val targetInfo = TargetInfoFixture.create()
            val member1 = MemberFixture.create(name = targetInfo.targetName, email = "hong1@example.com")
            val member2 = MemberFixture.create(name = targetInfo.targetName, email = "hong2@example.com")
            val member3 = MemberFixture.create(name = targetInfo.targetName, email = "hong3@example.com")
            val targetInfos = TargetInfos(listOf(targetInfo))

            every { memberQueryRepository.findByNames(targetInfos.extractTargetNames()) } returns listOf(member1, member2, member3)
            every { matchingResultRepository.findExistingMatchingResults(listOf(targetInfo.targetInfoId)) } returns emptyList()

            // When
            val result = processor.process(targetInfos.data)

            // Then
            result!! shouldHaveSize 3
            result[0].targetEmail shouldBe member1.email
            result[1].targetEmail shouldBe member2.email
            result[2].targetEmail shouldBe member3.email
            result.forEach { it.targetInfoId shouldBe targetInfo.targetInfoId }
        }

        test("같은 이름의 TargetInfo가 여러 개 있으면 각각 독립적으로 매칭된다") {
            // Given
            val targetInfo1 = TargetInfoFixture.create(targetInfoId = 1L, registerEmail = "user1@example.com")
            val targetInfo2 = TargetInfoFixture.create(targetInfoId = 2L, registerEmail = "user2@example.com")
            val member = MemberFixture.create(name = targetInfo1.targetName)
            val targetInfos = TargetInfos(listOf(targetInfo1, targetInfo2))

            every { memberQueryRepository.findByNames(targetInfos.extractTargetNames()) } returns listOf(member)
            every { matchingResultRepository.findExistingMatchingResults(listOf(targetInfo1.targetInfoId, targetInfo2.targetInfoId)) } returns emptyList()

            // When
            val result = processor.process(targetInfos.data)

            // Then
            result!! shouldHaveSize 2
            result[0].targetInfoId shouldBe targetInfo1.targetInfoId
            result[0].targetEmail shouldBe member.email
            result[1].targetInfoId shouldBe targetInfo2.targetInfoId
            result[1].targetEmail shouldBe member.email
        }

        test("빈 TargetInfo 목록이 입력되면 빈 결과를 반환한다") {
            // Given
            val targetInfos = TargetInfos(emptyList())

            every { memberQueryRepository.findByNames(emptySet()) } returns emptyList()
            every { matchingResultRepository.findExistingMatchingResults(emptyList()) } returns emptyList()

            // When
            val result = processor.process(targetInfos.data)

            // Then
            result!! shouldHaveSize 0
        }

        test("모든 매칭 결과가 기존에 존재하면 빈 결과를 반환한다") {
            // Given
            val targetInfo = TargetInfoFixture.create()
            val member1 = MemberFixture.create(name = targetInfo.targetName, email = "member1@example.com")
            val member2 = MemberFixture.create(name = targetInfo.targetName, email = "member2@example.com")
            val existingResult1 = MatchingResultFixture.create(targetInfoId = targetInfo.targetInfoId, targetEmail = member1.email)
            val existingResult2 = MatchingResultFixture.create(targetInfoId = targetInfo.targetInfoId, targetEmail = member2.email)
            val targetInfos = TargetInfos(listOf(targetInfo))

            every { memberQueryRepository.findByNames(targetInfos.extractTargetNames()) } returns listOf(member1, member2)
            every { matchingResultRepository.findExistingMatchingResults(listOf(targetInfo.targetInfoId)) } returns listOf(existingResult1, existingResult2)

            // When
            val result = processor.process(targetInfos.data)

            // Then
            result!! shouldHaveSize 0
        }
    }
})
