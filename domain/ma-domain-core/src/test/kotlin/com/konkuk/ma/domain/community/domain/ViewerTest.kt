package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.domain.block.BlockedMemberIds
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class ViewerTest : FunSpec({

    context("isLikedByMe") {

        test("내가 좋아요한 대상 id이면 true를 반환한다") {
            val likedTargetId = 10L
            val viewer = Viewer(viewerId = 1L, likedIds = LikedIds(setOf(likedTargetId)))

            viewer.isLikedByMe(likedTargetId).shouldBeTrue()
        }

        test("내가 좋아요하지 않은 대상 id이면 false를 반환한다") {
            val viewer = Viewer(viewerId = 1L, likedIds = LikedIds(setOf(10L)))

            viewer.isLikedByMe(999L).shouldBeFalse()
        }

        test("좋아요한 대상이 하나도 없으면 false를 반환한다") {
            val viewer = Viewer(viewerId = 1L, likedIds = LikedIds(emptySet()))

            viewer.isLikedByMe(10L).shouldBeFalse()
        }
    }

    context("isMine") {

        test("작성자 id가 조회자 자신이면 true를 반환한다") {
            val viewerId = 7L
            val viewer = Viewer(viewerId = viewerId, likedIds = LikedIds(emptySet()))

            viewer.isMine(viewerId).shouldBeTrue()
        }

        test("작성자 id가 조회자와 다르면 false를 반환한다") {
            val viewerId = 7L
            val viewer = Viewer(viewerId = viewerId, likedIds = LikedIds(emptySet()))

            viewer.isMine(viewerId + 1).shouldBeFalse()
        }
    }

    context("hasBlocked") {

        test("내가 차단한 작성자 id이면 true를 반환한다") {
            val blockedAuthorId = 20L
            val viewer = Viewer(
                viewerId = 1L,
                likedIds = LikedIds(emptySet()),
                blockedMemberIds = BlockedMemberIds(setOf(blockedAuthorId)),
            )

            viewer.hasBlocked(blockedAuthorId).shouldBeTrue()
        }

        test("내가 차단하지 않은 작성자 id이면 false를 반환한다") {
            val viewer = Viewer(
                viewerId = 1L,
                likedIds = LikedIds(emptySet()),
                blockedMemberIds = BlockedMemberIds(setOf(20L)),
            )

            viewer.hasBlocked(999L).shouldBeFalse()
        }

        test("차단 정보를 생략하고 만든 Viewer는 어떤 작성자도 차단하지 않은 것으로 본다") {
            val viewer = Viewer(viewerId = 1L, likedIds = LikedIds(emptySet()))

            viewer.hasBlocked(20L).shouldBeFalse()
        }
    }
})
