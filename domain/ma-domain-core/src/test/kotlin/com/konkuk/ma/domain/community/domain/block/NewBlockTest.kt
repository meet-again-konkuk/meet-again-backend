package com.konkuk.ma.domain.community.domain.block

import com.konkuk.ma.domain.community.fixture.NewBlockFixture
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NewBlockTest : FunSpec({

    context("생성") {

        test("차단자와 차단 대상이 서로 다르면 정상적으로 생성된다") {
            // Given
            val blockerId = 1L
            val blockedId = 2L

            // When
            val newBlock = NewBlockFixture.create(blockerId = blockerId, blockedId = blockedId)

            // Then
            newBlock.blockerId shouldBe blockerId
            newBlock.blockedId shouldBe blockedId
        }
    }

    context("본인 차단 검증") {

        test("차단자와 차단 대상이 같으면 InvalidValueException이 발생한다") {
            // Given
            val memberId = 7L

            // When & Then
            shouldThrow<InvalidValueException> {
                NewBlockFixture.create(blockerId = memberId, blockedId = memberId)
            }
        }
    }
})
