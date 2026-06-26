package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.matching.fixture.TargetInfoFixture
import com.konkuk.ma.domain.xroom.domain.FinalMessage
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import com.konkuk.ma.domain.xroom.fixture.XroomFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class XroomQueryServiceTest : FunSpec({

    val xroomQueryRepository = mockk<XroomQueryRepository>()
    val targetInfoQueryRepository = mockk<TargetInfoQueryRepository>()
    val xroomQueryService = XroomQueryService(xroomQueryRepository, targetInfoQueryRepository)

    fun xroom(id: Long, targetInfoId: Long) = XroomFixture.create(
        id = id,
        targetInfoId = targetInfoId,
        finalMessage = FinalMessage.of("고마웠어"),
    )

    context("findMine") {

        test("내가 만든 방 목록을 수신자 이름과 함께 반환한다") {
            val memberId = 1L
            every { xroomQueryRepository.find(memberId) } returns listOf(
                xroom(id = 1L, targetInfoId = 10L),
                xroom(id = 2L, targetInfoId = 20L),
            )
            every { targetInfoQueryRepository.find(memberId) } returns listOf(
                TargetInfoFixture.create(targetInfoId = 10L, targetName = "홍길동"),
                TargetInfoFixture.create(targetInfoId = 20L, targetName = "김철수"),
            )

            val result = xroomQueryService.findMine(memberId)

            result.data shouldHaveSize 2
            result.data[0].recipientName shouldBe "홍길동"
            result.data[1].recipientName shouldBe "김철수"
        }

        test("기억이 아직 없으므로 memoryCount는 0이다") {
            val memberId = 1L
            every { xroomQueryRepository.find(memberId) } returns listOf(xroom(id = 1L, targetInfoId = 10L))
            every { targetInfoQueryRepository.find(memberId) } returns listOf(
                TargetInfoFixture.create(targetInfoId = 10L, targetName = "홍길동")
            )

            val result = xroomQueryService.findMine(memberId)

            result.data[0].memoryCount shouldBe 0
        }

        test("만든 방이 없으면 빈 목록을 반환한다") {
            val memberId = 1L
            every { xroomQueryRepository.find(memberId) } returns emptyList()
            every { targetInfoQueryRepository.find(memberId) } returns emptyList()

            val result = xroomQueryService.findMine(memberId)

            result.data shouldHaveSize 0
        }
    }
})
