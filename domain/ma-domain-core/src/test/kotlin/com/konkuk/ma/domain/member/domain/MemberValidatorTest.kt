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

/**
 * 닉네임 중복 검사 정책 테스트.
 *
 * 핵심 계약은 "닉네임이 바뀌지 않으면 조회조차 하지 않는다" — 자기 닉네임을 그대로 보낸 요청이
 * 자기 자신 때문에 409 로 막히면 안 되고, 불필요한 조회도 하지 않아야 한다.
 */
class MemberValidatorTest : FunSpec({

    val memberQueryRepository = mockk<MemberQueryRepository>()
    val memberValidator = MemberValidator(NicknameValidator(memberQueryRepository))

    beforeEach {
        clearAllMocks()
    }

    context("validateNicknameAvailable") {

        test("닉네임 변경 지시가 없으면(null) 중복 조회를 하지 않는다") {
            // Given
            val current = MemberFixture.create(nickname = "기존닉네임")

            // When
            memberValidator.validateNicknameAvailable(current, null)

            // Then
            verify(exactly = 0) { memberQueryRepository.existsByNickname(any()) }
        }

        test("닉네임 변경 지시가 없으면(null) 예외를 던지지 않는다") {
            // Given
            val current = MemberFixture.create(nickname = "기존닉네임")

            // When & Then
            shouldNotThrowAny {
                memberValidator.validateNicknameAvailable(current, null)
            }
        }

        test("자기 닉네임을 그대로 보내면 중복 조회를 하지 않는다") {
            // Given
            val current = MemberFixture.create(nickname = "기존닉네임")

            // When
            memberValidator.validateNicknameAvailable(current, current.nickname)

            // Then
            verify(exactly = 0) { memberQueryRepository.existsByNickname(any()) }
        }

        test("자기 닉네임을 그대로 보내면 예외를 던지지 않는다") {
            // Given
            val current = MemberFixture.create(nickname = "기존닉네임")

            // When & Then
            shouldNotThrowAny {
                memberValidator.validateNicknameAvailable(current, current.nickname)
            }
        }

        test("닉네임이 바뀌고 중복이 없으면 통과한다") {
            // Given
            val current = MemberFixture.create(nickname = "기존닉네임")
            val newNickname = "새닉네임"
            every { memberQueryRepository.existsByNickname(newNickname) } returns false

            // When & Then
            shouldNotThrowAny {
                memberValidator.validateNicknameAvailable(current, newNickname)
            }
        }

        test("닉네임이 바뀌면 그 닉네임으로 중복 조회를 정확히 한 번 한다") {
            // Given
            val current = MemberFixture.create(nickname = "기존닉네임")
            val newNickname = "새닉네임"
            every { memberQueryRepository.existsByNickname(newNickname) } returns false

            // When
            memberValidator.validateNicknameAvailable(current, newNickname)

            // Then
            verify(exactly = 1) { memberQueryRepository.existsByNickname(newNickname) }
        }

        test("닉네임이 바뀌고 다른 회원이 이미 쓰고 있으면 DuplicateException을 던진다") {
            // Given
            val current = MemberFixture.create(nickname = "기존닉네임")
            val takenNickname = "선점닉네임"
            every { memberQueryRepository.existsByNickname(takenNickname) } returns true

            // When & Then
            shouldThrow<DuplicateException> {
                memberValidator.validateNicknameAvailable(current, takenNickname)
            }.message shouldBe "이미 사용중인 nickname입니다."
        }

        test("대소문자만 다른 닉네임도 변경으로 보고 중복 조회를 한다") {
            // Given — 닉네임 비교는 정확 일치다
            val current = MemberFixture.create(nickname = "nickname")
            val differentCase = "NICKNAME"
            every { memberQueryRepository.existsByNickname(differentCase) } returns false

            // When
            memberValidator.validateNicknameAvailable(current, differentCase)

            // Then
            verify(exactly = 1) { memberQueryRepository.existsByNickname(differentCase) }
        }

        test("앞뒤 공백만 다른 닉네임도 변경으로 보고 중복 조회를 한다") {
            // Given — 트리밍하지 않는다
            val current = MemberFixture.create(nickname = "닉네임")
            val padded = " 닉네임 "
            every { memberQueryRepository.existsByNickname(padded) } returns false

            // When
            memberValidator.validateNicknameAvailable(current, padded)

            // Then
            verify(exactly = 1) { memberQueryRepository.existsByNickname(padded) }
        }
    }
})
