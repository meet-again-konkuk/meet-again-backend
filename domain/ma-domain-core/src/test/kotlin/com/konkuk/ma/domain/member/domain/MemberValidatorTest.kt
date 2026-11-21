package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.matching.fixture.MemberFixture
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

class MemberValidatorTest : FunSpec({

    val memberQueryRepository = mockk<MemberQueryRepository>()
    val memberValidator = MemberValidator(NicknameValidator(memberQueryRepository))

    beforeEach {
        clearAllMocks()
    }

    context("validateNicknameAvailable") {

        test("닉네임 변경 지시가 없으면(null) 중복 조회를 하지 않는다") {
            val current = MemberFixture.create(nickname = "기존닉네임")

            memberValidator.validateNicknameAvailable(current, null)

            verify(exactly = 0) { memberQueryRepository.existsByNickname(any()) }
        }

        test("닉네임 변경 지시가 없으면(null) 예외를 던지지 않는다") {
            val current = MemberFixture.create(nickname = "기존닉네임")

            shouldNotThrowAny {
                memberValidator.validateNicknameAvailable(current, null)
            }
        }

        test("자기 닉네임을 그대로 보내면 중복 조회를 하지 않는다") {
            val current = MemberFixture.create(nickname = "기존닉네임")

            memberValidator.validateNicknameAvailable(current, current.nickname)

            verify(exactly = 0) { memberQueryRepository.existsByNickname(any()) }
        }

        test("자기 닉네임을 그대로 보내면 예외를 던지지 않는다") {
            val current = MemberFixture.create(nickname = "기존닉네임")

            shouldNotThrowAny {
                memberValidator.validateNicknameAvailable(current, current.nickname)
            }
        }

        test("닉네임이 바뀌고 중복이 없으면 통과한다") {
            val current = MemberFixture.create(nickname = "기존닉네임")
            val newNickname = "새닉네임"
            every { memberQueryRepository.existsByNickname(newNickname) } returns false

            shouldNotThrowAny {
                memberValidator.validateNicknameAvailable(current, newNickname)
            }
        }

        test("닉네임이 바뀌면 그 닉네임으로 중복 조회를 정확히 한 번 한다") {
            val current = MemberFixture.create(nickname = "기존닉네임")
            val newNickname = "새닉네임"
            every { memberQueryRepository.existsByNickname(newNickname) } returns false

            memberValidator.validateNicknameAvailable(current, newNickname)

            verify(exactly = 1) { memberQueryRepository.existsByNickname(newNickname) }
        }

        test("닉네임이 바뀌고 다른 회원이 이미 쓰고 있으면 DuplicateException을 던진다") {
            val current = MemberFixture.create(nickname = "기존닉네임")
            val takenNickname = "선점닉네임"
            every { memberQueryRepository.existsByNickname(takenNickname) } returns true

            shouldThrow<DuplicateException> {
                memberValidator.validateNicknameAvailable(current, takenNickname)
            }.message shouldBe "이미 사용중인 nickname입니다."
        }

        test("대소문자만 다른 닉네임도 변경으로 보고 중복 조회를 한다") {
            val current = MemberFixture.create(nickname = "nickname")
            val differentCase = "NICKNAME"
            every { memberQueryRepository.existsByNickname(differentCase) } returns false

            memberValidator.validateNicknameAvailable(current, differentCase)

            verify(exactly = 1) { memberQueryRepository.existsByNickname(differentCase) }
        }

        test("앞뒤 공백만 다른 닉네임도 변경으로 보고 중복 조회를 한다") {
            val current = MemberFixture.create(nickname = "닉네임")
            val padded = " 닉네임 "
            every { memberQueryRepository.existsByNickname(padded) } returns false

            memberValidator.validateNicknameAvailable(current, padded)

            verify(exactly = 1) { memberQueryRepository.existsByNickname(padded) }
        }
    }
})
