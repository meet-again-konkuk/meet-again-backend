package com.konkuk.ma.domain.member.entity

import com.konkuk.ma.member.domain.Member
import java.time.LocalDate

class MemberEntity(
    val email: String,

    val password: String,

    val nickname: String,

    val phoneNumber: String,

    val name: String,

    val birthDate: LocalDate,

    val highSchool: String?,

    val university: String?
) {
    fun toDomain(): Member {
        return Member.create(
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
