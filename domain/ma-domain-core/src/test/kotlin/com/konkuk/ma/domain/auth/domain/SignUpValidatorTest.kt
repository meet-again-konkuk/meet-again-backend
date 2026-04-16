package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.auth.domain.port.SmsRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.member.exception.SmsNotVerifiedException
import com.konkuk.ma.domain.member.fixture.NewMemberFixture
import com.konkuk.ma.exception.DuplicateException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class SignUpValidatorTest : FunSpec({

    val memberQueryRepository = mockk<MemberQueryRepository>()
    val smsRepository = mockk<SmsRepository>()
    val signUpValidator = SignUpValidator(memberQueryRepository, smsRepository)

    context("validate") {
        test("모든 검증이 통과하면 예외가 발생하지 않는다") {
            // Given
            val newMember = NewMemberFixture.create()

            every { memberQueryRepository.existsByNickname(newMember.nickname) } returns false
            every { memberQueryRepository.exists(newMember.email) } returns false
            every { smsRepository.getConfirmed(newMember.phoneNumber.fullNumber) } returns true

            // When & Then
            signUpValidator.validate(newMember)
        }

        test("중복된 닉네임이 있으면 예외가 발생한다") {
            // Given
            val newMember = NewMemberFixture.create()

            every { memberQueryRepository.existsByNickname(newMember.nickname) } returns true

            // When & Then
            shouldThrow<DuplicateException> {
                signUpValidator.validate(newMember)
            }.message shouldBe "이미 사용중인 nickname입니다."
        }

        test("중복된 이메일이 있으면 예외가 발생한다") {
            // Given
            val newMember = NewMemberFixture.create()

            every { memberQueryRepository.existsByNickname(newMember.nickname) } returns false
            every { memberQueryRepository.exists(newMember.email) } returns true

            // When & Then
            shouldThrow<DuplicateException> {
                signUpValidator.validate(newMember)
            }.message shouldBe "이미 사용중인 email입니다."
        }

        test("SMS 인증이 완료되지 않으면 예외가 발생한다") {
            // Given
            val newMember = NewMemberFixture.create()

            every { memberQueryRepository.existsByNickname(newMember.nickname) } returns false
            every { memberQueryRepository.exists(newMember.email) } returns false
            every { smsRepository.getConfirmed(newMember.phoneNumber.fullNumber) } returns false

            // When & Then
            shouldThrow<SmsNotVerifiedException> {
                signUpValidator.validate(newMember)
            }.message shouldBe SmsNotVerifiedException.MESSAGE
        }

        test("여러 검증 실패 시 첫 번째 실패한 검증의 예외가 발생한다") {
            // Given
            val newMember = NewMemberFixture.create()

            every { memberQueryRepository.existsByNickname(newMember.nickname) } returns true
            every { memberQueryRepository.exists(newMember.email) } returns true
            every { smsRepository.getConfirmed(newMember.phoneNumber.fullNumber) } returns false

            // When & Then
            shouldThrow<DuplicateException> {
                signUpValidator.validate(newMember)
            }.message shouldBe "이미 사용중인 nickname입니다."
        }
    }
})
