package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.exception.DuplicateException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class NicknameValidatorTest : FunSpec({

    val memberQueryRepository = mockk<MemberQueryRepository>()
    val nicknameValidator = NicknameValidator(memberQueryRepository)

    beforeEach {
        clearAllMocks()
    }

    context("validate") {

        test("아무도 쓰고 있지 않으면 통과한다") {
            val nickname = "새닉네임"
            every { memberQueryRepository.existsByNickname(nickname) } returns false

            shouldNotThrowAny {
                nicknameValidator.validate(nickname)
            }
        }

        test("이미 쓰고 있는 닉네임이면 DuplicateException을 던진다") {
            val nickname = "선점닉네임"
            every { memberQueryRepository.existsByNickname(nickname) } returns true

            shouldThrow<DuplicateException> {
                nicknameValidator.validate(nickname)
            }.message shouldBe "이미 사용중인 nickname입니다."
        }

        test("받은 닉네임 그대로 정확히 한 번 조회한다") {
            val nickname = " NickName "
            every { memberQueryRepository.existsByNickname(nickname) } returns false

            nicknameValidator.validate(nickname)

            verify(exactly = 1) { memberQueryRepository.existsByNickname(nickname) }
        }
    }
})
