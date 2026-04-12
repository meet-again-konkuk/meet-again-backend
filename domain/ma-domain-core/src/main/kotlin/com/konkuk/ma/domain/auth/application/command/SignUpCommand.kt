package com.konkuk.ma.domain.auth.application.command

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.domain.PhoneNumber
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.domain.auth.domain.port.PasswordEncryptor
import java.time.LocalDate

class SignUpCommand(
    email: String,
    val password: String,
    val nickname: String,
    val gender: Gender,
    val phoneNumber: String,
    val name: String,
    val birthDate: LocalDate,
    val region: Region,
    val highSchool: String?,
    val university: String?
) {
    val email: Email = Email(email)

    fun toNewMember(passwordEncryptor: PasswordEncryptor): NewMember {
        return NewMember(
            email = this.email,
            password = passwordEncryptor.encode(password),
            nickname = nickname,
            gender = gender,
            phoneNumber = PhoneNumber(phoneNumber),
            name = name,
            birthDate = birthDate,
            region = region,
            highSchool = highSchool,
            university = university
        )
    }
}
