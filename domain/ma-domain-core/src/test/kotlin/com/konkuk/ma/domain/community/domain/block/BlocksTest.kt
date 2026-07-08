package com.konkuk.ma.domain.community.domain.block

import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.Members
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class BlocksTest : FunSpec({

    context("extractBlockedIds") {

        test("차단 대상 회원 id 집합을 추출한다") {
            // Given
            val blocks = Blocks(
                listOf(
                    Block(id = 1L, blockerId = 1L, blockedId = 10L),
                    Block(id = 2L, blockerId = 1L, blockedId = 20L),
                ),
            )

            // When & Then
            blocks.extractBlockedIds() shouldContainExactlyInAnyOrder setOf(10L, 20L)
        }

        test("같은 대상이 여러 번 있어도 중복 없이 하나로 집계된다") {
            // Given
            val duplicatedBlockedId = 10L
            val blocks = Blocks(
                listOf(
                    Block(id = 1L, blockerId = 1L, blockedId = duplicatedBlockedId),
                    Block(id = 2L, blockerId = 1L, blockedId = duplicatedBlockedId),
                ),
            )

            // When & Then
            blocks.extractBlockedIds() shouldContainExactly setOf(duplicatedBlockedId)
        }

        test("차단이 하나도 없으면 빈 집합을 반환한다") {
            // Given
            val blocks = Blocks(emptyList())

            // When & Then
            blocks.extractBlockedIds() shouldHaveSize 0
        }
    }

    context("combineWithViews") {

        test("각 차단을 차단 대상 닉네임을 담은 BlockView 목록으로 변환한다") {
            // Given
            val firstBlocked = MemberFixture.create(id = 10L, nickname = "첫번째차단대상")
            val secondBlocked = MemberFixture.create(id = 20L, nickname = "두번째차단대상")
            val members = Members(listOf(firstBlocked, secondBlocked))
            val blocks = Blocks(
                listOf(
                    Block(id = 1L, blockerId = 1L, blockedId = firstBlocked.id),
                    Block(id = 2L, blockerId = 1L, blockedId = secondBlocked.id),
                ),
            )

            // When
            val views = blocks.combineWithViews(members)

            // Then
            views shouldHaveSize 2
            views[0].nickname shouldBe firstBlocked.nickname
            views[1].nickname shouldBe secondBlocked.nickname
        }

        test("차단이 없으면 빈 목록을 반환한다") {
            // Given
            val blocks = Blocks(emptyList())

            // When & Then
            blocks.combineWithViews(Members(emptyList())) shouldHaveSize 0
        }
    }
})
