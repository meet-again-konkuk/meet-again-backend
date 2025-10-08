package com.konkuk.ma.domain.member.application.command

import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.domain.member.domain.port.PasswordEncryptor
import java.time.LocalDate

data class NewMemberCommand(
    val email: String,
    val password: String,
    val nickname: String,
    val phoneNumber: String,
    val name: String,
    val birthDate: LocalDate,
    val region: Region,
    val highSchool: String?,
    val university: String?
) {
    fun toNewMember(passwordEncryptor: PasswordEncryptor): NewMember {
        return NewMember(
            email = email,
            password = passwordEncryptor.encode(password),
            nickname = nickname,
            phoneNumber = phoneNumber,
            name = name,
            birthDate = birthDate,
            region = region,
            highSchool = highSchool,
            university = university
        )
    }
}
