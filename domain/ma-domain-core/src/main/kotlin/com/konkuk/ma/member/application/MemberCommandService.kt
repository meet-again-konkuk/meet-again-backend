package com.konkuk.ma.member.application

import com.konkuk.ma.member.application.command.NewMemberCommand
import com.konkuk.ma.member.domain.MemberValidator
import com.konkuk.ma.member.domain.port.MemberCommandRepository
import com.konkuk.ma.member.domain.port.PasswordEncryptor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberCommandService(
    private val memberCommandRepository: MemberCommandRepository,
    private val memberValidator: MemberValidator,
    private val passwordEncryptor: PasswordEncryptor
) {
    fun signUp(newMemberCommand: NewMemberCommand): Long {
        val newMember = newMemberCommand.toNewMember(passwordEncryptor)
        memberValidator.validateNewMember(newMember)
        return memberCommandRepository.save(newMember)
    }
}
