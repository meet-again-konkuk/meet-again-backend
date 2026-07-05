package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.common.domain.Email
import java.time.LocalDate

class NewMember(
    val email: Email,
    val password: String,
    val nickname: String,
    val gender: Gender,
    val phoneNumber: PhoneNumber,
    val name: String,
    val birthDate: LocalDate,
    val region: Region,
    val highSchool: String?,
    val university: String?
) {
    fun withEncodedPassword(encodedPassword: String): NewMember {
        return NewMember(
            email = email,
            password = encodedPassword,
            nickname = nickname,
            gender = gender,
            phoneNumber = phoneNumber,
            name = name,
            birthDate = birthDate,
            region = region,
            highSchool = highSchool,
            university = university
        )
    }

    companion object {
        fun of(
            email: String,
            password: String,
            nickname: String,
            gender: Gender,
            phoneNumber: String,
            name: String,
            birthDate: LocalDate,
            region: Region,
            highSchool: String?,
            university: String?
        ): NewMember {
            return NewMember(
                email = Email(email),
                password = password,
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
}
