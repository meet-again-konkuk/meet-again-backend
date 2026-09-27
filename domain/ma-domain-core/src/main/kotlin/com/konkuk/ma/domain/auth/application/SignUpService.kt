package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.domain.SignUpValidator
import com.konkuk.ma.domain.auth.domain.port.PasswordEncryptor
import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SignUpService(
    private val memberCommandRepository: MemberCommandRepository,
    private val signUpValidator: SignUpValidator,
    private val passwordEncryptor: PasswordEncryptor
) {
    fun signUp(newMember: NewMember): Long {
        val securedMember = newMember.withEncodedPassword(passwordEncryptor.encode(newMember.password))
        signUpValidator.validate(securedMember)
        return memberCommandRepository.save(securedMember)
    }
}
