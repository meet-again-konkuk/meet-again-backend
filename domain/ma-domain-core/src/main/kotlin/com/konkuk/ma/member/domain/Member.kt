package com.konkuk.ma.member.domain

import java.time.LocalDate

class Member(
    val email: String,
    val password: String,
    val nickname: String,
    val phoneNumber: String,
    val name: String,
    val birthDate: LocalDate,
    val highSchool: String?,
    val university: String?
) {
    companion object {
        fun create(
            email: String,
            password: String,
            nickname: String,
            phoneNumber: String,
            name: String,
            birthDate: LocalDate,
            highSchool: String?,
            university: String?
        ): Member {
            return Member(
                email = email,
                password = password,
                nickname = nickname,
                phoneNumber = phoneNumber,
                name = name,
                birthDate = birthDate,
                highSchool = highSchool,
                university = university
            )
        }
    }
} 