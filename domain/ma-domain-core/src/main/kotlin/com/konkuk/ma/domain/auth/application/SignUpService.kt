package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.application.command.SignUpCommand
import com.konkuk.ma.domain.auth.domain.SignUpValidator
import com.konkuk.ma.domain.auth.domain.port.PasswordEncryptor
import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoUploader
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SignUpService(
    private val memberCommandRepository: MemberCommandRepository,
    private val signUpValidator: SignUpValidator,
    private val passwordEncryptor: PasswordEncryptor,
    private val memberPhotoUploader: MemberPhotoUploader
) {
    fun signUp(signUpCommand: SignUpCommand, photoFile: PhotoFile? = null): Long {
        val newMember = signUpCommand.toNewMember(passwordEncryptor)
        signUpValidator.validate(newMember)
        val memberId = memberCommandRepository.save(newMember)
        memberPhotoUploader.upload(newMember.email, photoFile)
        return memberId
    }
}
