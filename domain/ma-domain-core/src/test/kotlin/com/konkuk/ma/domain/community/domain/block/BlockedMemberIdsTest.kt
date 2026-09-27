package com.konkuk.ma.domain.community.domain.block

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class BlockedMemberIdsTest : FunSpec({

    context("contains") {

        test("차단 집합에 포함된 작성자 id이면 true를 반환한다") {
            // Given
            val blockedMemberIds = BlockedMemberIds(setOf(10L, 20L, 30L))

            // When & Then
            blockedMemberIds.contains(20L).shouldBeTrue()
        }

        test("차단 집합에 없는 작성자 id이면 false를 반환한다") {
            // Given
            val blockedMemberIds = BlockedMemberIds(setOf(10L, 20L, 30L))

            // When & Then
            blockedMemberIds.contains(999L).shouldBeFalse()
        }

        test("차단 집합이 비어있으면 어떤 id를 조회해도 false를 반환한다") {
            // Given
            val blockedMemberIds = BlockedMemberIds(emptySet())

            // When & Then
            blockedMemberIds.contains(10L).shouldBeFalse()
        }
    }
})
