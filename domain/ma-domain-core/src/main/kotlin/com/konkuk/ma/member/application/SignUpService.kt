package com.konkuk.ma.member.application

import com.konkuk.ma.member.domain.Member
import com.konkuk.ma.member.domain.MemberCommandRepository
import com.konkuk.ma.member.domain.MemberValidator
import com.konkuk.ma.member.domain.port.PasswordEncryptor
import java.time.LocalDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SignUpService(
    private val memberCommandRepository: MemberCommandRepository,
    private val memberValidator: MemberValidator,
    private val passwordEncryptor: PasswordEncryptor
) {
    fun signUp(
        email: String,
        password: String,
        nickname: String,
        phoneNumber: String,
        name: String,
        birthDate: LocalDate,
        highSchool: String?,
        university: String?
    ): Long {
        memberValidator.checkDuplicatedEmail(email)
        memberValidator.checkDuplicatedNickname(nickname)
        memberValidator.checkSmsVerification(phoneNumber)
        
        val encodedPassword = passwordEncryptor.encode(password)
        
        val member = Member.create(
            email = email,
            password = encodedPassword,
            nickname = nickname,
            phoneNumber = phoneNumber,
            name = name,
            birthDate = birthDate,
            highSchool = highSchool,
            university = university
        )
        
        return memberCommandRepository.save(member)
    }
}
