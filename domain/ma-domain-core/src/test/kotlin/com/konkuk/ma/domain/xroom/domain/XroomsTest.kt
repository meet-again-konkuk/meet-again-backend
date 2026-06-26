package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.matching.domain.TargetInfos
import com.konkuk.ma.domain.matching.fixture.TargetInfoFixture
import com.konkuk.ma.domain.xroom.fixture.XroomFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class XroomsTest : FunSpec({

    context("toMine") {

        test("각 방의 수신자 이름을 targetInfo에서 채워 MyXrooms로 변환한다") {
            val xrooms = Xrooms(
                listOf(
                    XroomFixture.create(id = 1L, targetInfoId = 10L),
                    XroomFixture.create(id = 2L, targetInfoId = 20L),
                )
            )
            val targetInfos = TargetInfos(
                listOf(
                    TargetInfoFixture.create(targetInfoId = 10L, targetName = "홍길동"),
                    TargetInfoFixture.create(targetInfoId = 20L, targetName = "김철수"),
                )
            )

            val mine = xrooms.toMine(targetInfos, emptyMap())

            mine.data shouldHaveSize 2
            mine.data[0].recipientName shouldBe "홍길동"
            mine.data[1].recipientName shouldBe "김철수"
        }

        test("기억 개수가 집계되지 않은 방은 memoryCount가 0이다") {
            val xrooms = Xrooms(listOf(XroomFixture.create(id = 1L, targetInfoId = 10L)))
            val targetInfos = TargetInfos(
                listOf(TargetInfoFixture.create(targetInfoId = 10L, targetName = "홍길동"))
            )

            val mine = xrooms.toMine(targetInfos, emptyMap())

            mine.data[0].memoryCount shouldBe 0
        }

        test("집계된 기억 개수가 있으면 해당 값을 채운다") {
            val xrooms = Xrooms(listOf(XroomFixture.create(id = 1L, targetInfoId = 10L)))
            val targetInfos = TargetInfos(
                listOf(TargetInfoFixture.create(targetInfoId = 10L, targetName = "홍길동"))
            )

            val mine = xrooms.toMine(targetInfos, mapOf(1L to 3))

            mine.data[0].memoryCount shouldBe 3
        }

        test("방이 없으면 빈 MyXrooms를 반환한다") {
            val mine = Xrooms(emptyList()).toMine(TargetInfos(emptyList()), emptyMap())

            mine.data shouldHaveSize 0
        }
    }
})
