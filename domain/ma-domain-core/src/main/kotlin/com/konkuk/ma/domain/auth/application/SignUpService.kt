package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.application.command.SignUpCommand
import com.konkuk.ma.domain.auth.domain.SignUpValidator
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.auth.domain.port.PasswordEncryptor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SignUpService(
    private val memberCommandRepository: MemberCommandRepository,
    private val signUpValidator: SignUpValidator,
    private val passwordEncryptor: PasswordEncryptor
) {
    fun signUp(signUpCommand: SignUpCommand): Long {
        val newMember = signUpCommand.toNewMember(passwordEncryptor)
        signUpValidator.validate(newMember)
        return memberCommandRepository.save(newMember)
    }
}
