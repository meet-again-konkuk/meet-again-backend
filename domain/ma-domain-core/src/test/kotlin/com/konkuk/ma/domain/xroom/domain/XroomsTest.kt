package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.matching.domain.TargetInfos
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.matching.fixture.TargetInfoFixture
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.xroom.domain.memory.MemoryCounts
import com.konkuk.ma.domain.xroom.fixture.XroomFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class XroomsTest : FunSpec({

    context("extractOwnerIds") {

        test("여러 방의 ownerId 집합을 반환한다") {
            val xrooms = Xrooms(
                listOf(
                    XroomFixture.create(id = 1L, ownerId = 1L),
                    XroomFixture.create(id = 2L, ownerId = 2L),
                )
            )

            xrooms.extractOwnerIds() shouldContainExactlyInAnyOrder setOf(1L, 2L)
        }

        test("중복된 ownerId는 하나로 합쳐진다") {
            val xrooms = Xrooms(
                listOf(
                    XroomFixture.create(id = 1L, ownerId = 1L),
                    XroomFixture.create(id = 2L, ownerId = 1L),
                )
            )

            xrooms.extractOwnerIds() shouldContainExactlyInAnyOrder setOf(1L)
        }

        test("방이 없으면 빈 집합을 반환한다") {
            Xrooms(emptyList()).extractOwnerIds() shouldBe emptySet()
        }
    }

    context("extractIds") {

        test("여러 방의 id 집합을 반환한다") {
            val xrooms = Xrooms(
                listOf(
                    XroomFixture.create(id = 1L),
                    XroomFixture.create(id = 2L),
                )
            )

            xrooms.extractIds() shouldContainExactlyInAnyOrder setOf(1L, 2L)
        }

        test("방이 없으면 빈 집합을 반환한다") {
            Xrooms(emptyList()).extractIds() shouldBe emptySet()
        }
    }

    context("toReceived") {

        test("각 방의 보낸 사람 이름을 senders에서 채워 ReceivedXrooms로 변환한다") {
            val sender1 = MemberFixture.create(id = 1L, name = "김보냄")
            val sender2 = MemberFixture.create(id = 2L, name = "이전달")
            val senders = Members(listOf(sender1, sender2))
            val xrooms = Xrooms(
                listOf(
                    XroomFixture.create(id = 1L, ownerId = sender1.id),
                    XroomFixture.create(id = 2L, ownerId = sender2.id),
                )
            )

            val received = xrooms.toReceived(senders, MemoryCounts(emptyMap()))

            received.data shouldHaveSize 2
            received.data[0].senderName shouldBe sender1.name
            received.data[1].senderName shouldBe sender2.name
        }

        test("senders에 없는 ownerId면 보낸 사람 이름은 '알 수 없음'이다") {
            val xrooms = Xrooms(listOf(XroomFixture.create(id = 1L, ownerId = 999L)))

            val received = xrooms.toReceived(Members(emptyList()), MemoryCounts(emptyMap()))

            received.data[0].senderName shouldBe "알 수 없음"
        }

        test("MemoryCounts에 값이 있는 방은 그 수를, 없는 방은 0을 memoryCount로 채운다") {
            val sender = MemberFixture.create(id = 1L, name = "김보냄")
            val withMemories = XroomFixture.create(id = 1L, ownerId = sender.id)
            val withoutMemories = XroomFixture.create(id = 2L, ownerId = sender.id)
            val xrooms = Xrooms(listOf(withMemories, withoutMemories))
            val memoryCounts = MemoryCounts(mapOf(withMemories.id to 3))

            val received = xrooms.toReceived(Members(listOf(sender)), memoryCounts)

            received.data[0].memoryCount shouldBe 3
            received.data[1].memoryCount shouldBe 0
        }

        test("방이 없으면 빈 ReceivedXrooms를 반환한다") {
            val received = Xrooms(emptyList()).toReceived(Members(emptyList()), MemoryCounts(emptyMap()))

            received.data shouldHaveSize 0
        }
    }

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

            val mine = xrooms.toMine(targetInfos, MemoryCounts(emptyMap()))

            mine.data shouldHaveSize 2
            mine.data[0].recipientName shouldBe "홍길동"
            mine.data[1].recipientName shouldBe "김철수"
        }

        test("MemoryCounts에 값이 있는 방은 그 수를, 없는 방은 0을 memoryCount로 채운다") {
            val withMemories = XroomFixture.create(id = 1L, targetInfoId = 10L)
            val withoutMemories = XroomFixture.create(id = 2L, targetInfoId = 20L)
            val xrooms = Xrooms(listOf(withMemories, withoutMemories))
            val targetInfos = TargetInfos(
                listOf(
                    TargetInfoFixture.create(targetInfoId = 10L, targetName = "홍길동"),
                    TargetInfoFixture.create(targetInfoId = 20L, targetName = "김철수"),
                )
            )
            val memoryCounts = MemoryCounts(mapOf(withMemories.id to 3))

            val mine = xrooms.toMine(targetInfos, memoryCounts)

            mine.data[0].memoryCount shouldBe 3
            mine.data[1].memoryCount shouldBe 0
        }

        test("방이 없으면 빈 MyXrooms를 반환한다") {
            val mine = Xrooms(emptyList()).toMine(TargetInfos(emptyList()), MemoryCounts(emptyMap()))

            mine.data shouldHaveSize 0
        }
    }
})
