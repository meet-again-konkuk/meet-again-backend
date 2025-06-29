package com.konkuk.ma.member.domain

import com.konkuk.ma.auth.domain.port.SmsRepository
import com.konkuk.ma.member.domain.port.MemberQueryRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class MemberValidatorTest : FunSpec({
    
    val memberQueryRepository = mockk<MemberQueryRepository>()
    val smsRepository = mockk<SmsRepository>()
    val memberValidator = MemberValidator(memberQueryRepository, smsRepository)
    
    context("validateNewMember") {
        test("모든 검증이 통과하면 예외가 발생하지 않는다") {
            // Given
            val newMember = NewMember(
                email = "test@example.com",
                password = "password123",
                nickname = "testuser",
                phoneNumber = "01012345678",
                name = "김테스트",
                birthDate = LocalDate.of(1990, 1, 1),
                highSchool = "테스트고등학교",
                university = "테스트대학교"
            )
            
            every { memberQueryRepository.existsByNickname("testuser") } returns false
            every { memberQueryRepository.existsByEmail("test@example.com") } returns false
            every { smsRepository.getConfirmed("01012345678") } returns true
            
            // When & Then
            memberValidator.validateNewMember(newMember)
        }
        
        test("중복된 닉네임이 있으면 예외가 발생한다") {
            // Given
            val newMember = NewMember(
                email = "test@example.com",
                password = "password123",
                nickname = "duplicated",
                phoneNumber = "01012345678",
                name = "김테스트",
                birthDate = LocalDate.of(1990, 1, 1),
                highSchool = null,
                university = null
            )
            
            every { memberQueryRepository.existsByNickname("duplicated") } returns true
            
            // When & Then
            val exception = shouldThrow<IllegalArgumentException> {
                memberValidator.validateNewMember(newMember)
            }
            exception.message shouldBe "이미 사용중인 닉네임입니다."
        }
        
        test("중복된 이메일이 있으면 예외가 발생한다") {
            // Given
            val newMember = NewMember(
                email = "duplicated@example.com",
                password = "password123",
                nickname = "testuser",
                phoneNumber = "01012345678",
                name = "김테스트",
                birthDate = LocalDate.of(1990, 1, 1),
                highSchool = null,
                university = null
            )
            
            every { memberQueryRepository.existsByNickname("testuser") } returns false
            every { memberQueryRepository.existsByEmail("duplicated@example.com") } returns true
            
            // When & Then
            val exception = shouldThrow<IllegalArgumentException> {
                memberValidator.validateNewMember(newMember)
            }
            exception.message shouldBe "이미 사용중인 이메일입니다."
        }
        
        test("SMS 인증이 완료되지 않으면 예외가 발생한다") {
            // Given
            val newMember = NewMember(
                email = "test@example.com",
                password = "password123",
                nickname = "testuser",
                phoneNumber = "01012345678",
                name = "김테스트",
                birthDate = LocalDate.of(1990, 1, 1),
                highSchool = null,
                university = null
            )
            
            every { memberQueryRepository.existsByNickname("testuser") } returns false
            every { memberQueryRepository.existsByEmail("test@example.com") } returns false
            every { smsRepository.getConfirmed("01012345678") } returns false
            
            // When & Then
            val exception = shouldThrow<IllegalArgumentException> {
                memberValidator.validateNewMember(newMember)
            }
            exception.message shouldBe "휴대폰 번호 인증이 완료되지 않았습니다."
        }
        
        test("여러 검증 실패 시 첫 번째 실패한 검증의 예외가 발생한다") {
            // Given
            val newMember = NewMember(
                email = "duplicated@example.com",
                password = "password123",
                nickname = "duplicated",
                phoneNumber = "01012345678",
                name = "김테스트",
                birthDate = LocalDate.of(1990, 1, 1),
                highSchool = null,
                university = null
            )
            
            every { memberQueryRepository.existsByNickname("duplicated") } returns true
            every { memberQueryRepository.existsByEmail("duplicated@example.com") } returns true
            every { smsRepository.getConfirmed("01012345678") } returns false
            
            // When & Then
            val exception = shouldThrow<IllegalArgumentException> {
                memberValidator.validateNewMember(newMember)
            }
            // 닉네임 검증이 먼저 실행되므로 닉네임 관련 예외가 발생
            exception.message shouldBe "이미 사용중인 닉네임입니다."
        }
    }
}) 