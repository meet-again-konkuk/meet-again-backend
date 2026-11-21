package com.konkuk.ma.domain.community.domain.block

import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.exception.AccessDeniedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class BlockTest : FunSpec({

    context("validateOwnership") {

        test("차단을 만든 본인이면 예외 없이 통과한다") {
            val blockerId = 1L
            val block = Block(blockerId = blockerId, blockedId = 2L)

            block.validateOwnership(blockerId)
        }

        test("차단을 만든 본인이 아니면 AccessDeniedException이 발생한다") {
            val block = Block(blockerId = 1L, blockedId = 2L)
            val otherMemberId = 999L

            shouldThrow<AccessDeniedException> {
                block.validateOwnership(otherMemberId)
            }.message shouldBe "CommunityBlock에 대한 접근 권한이 없습니다."
        }
    }

    context("combineWithView") {

        test("차단당한 회원(blockedId)의 닉네임과 차단 시각으로 BlockView를 만든다") {
            val blockedAt = LocalDateTime.of(2026, 7, 7, 12, 0)
            val blocker = MemberFixture.create(id = 1L, nickname = "차단자")
            val blocked = MemberFixture.create(id = 2L, nickname = "차단당한사람")
            val members = Members(listOf(blocker, blocked))
            val block = Block(id = 30L, blockerId = blocker.id, blockedId = blocked.id, createdDate = blockedAt)

            val view = block.combineWithView(members)

            view.blockId shouldBe block.id
            view.nickname shouldBe blocked.nickname
            view.blockedAt shouldBe blockedAt
        }
    }
})
