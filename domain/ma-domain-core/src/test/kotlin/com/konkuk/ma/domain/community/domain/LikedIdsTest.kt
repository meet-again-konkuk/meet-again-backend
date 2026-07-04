package com.konkuk.ma.domain.community.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class LikedIdsTest : FunSpec({

    context("contains") {

        test("집합에 포함된 id이면 true를 반환한다") {
            // Given
            val likedIds = LikedIds(setOf(1L, 2L, 3L))

            // When & Then
            likedIds.contains(2L).shouldBeTrue()
        }

        test("집합에 없는 id이면 false를 반환한다") {
            // Given
            val likedIds = LikedIds(setOf(1L, 2L, 3L))

            // When & Then
            likedIds.contains(999L).shouldBeFalse()
        }

        test("빈 집합에서는 어떤 id를 조회해도 false를 반환한다") {
            // Given
            val likedIds = LikedIds(emptySet())

            // When & Then
            likedIds.contains(1L).shouldBeFalse()
        }

        test("단일 원소 집합에서 그 원소는 true, 다른 값은 false를 반환한다") {
            // Given
            val likedId = 42L
            val likedIds = LikedIds(setOf(likedId))

            // When & Then
            likedIds.contains(likedId).shouldBeTrue()
            likedIds.contains(likedId + 1).shouldBeFalse()
        }
    }
})
