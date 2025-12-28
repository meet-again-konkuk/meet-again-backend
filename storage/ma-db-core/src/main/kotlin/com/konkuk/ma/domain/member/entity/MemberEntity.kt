package com.konkuk.ma.domain.member.entity

import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.Region
import java.time.LocalDate

class MemberEntity(
    val id: Long,

    val email: String,

    val password: String,

    val nickname: String,

    val gender: Gender,

    val phoneNumber: String,

    val name: String,

    val region: Region,

    val birthDate: LocalDate,

    val highSchool: String?,

    val university: String?
) {
    fun toDomain(): Member {
        return Member.create(
            email = email,
            password = password,
            nickname = nickname,
            gender = gender,
            phoneNumber = phoneNumber,
            name = name,
            region = region,
            birthDate = birthDate,
            highSchool = highSchool,
            university = university
        )
    }
}
