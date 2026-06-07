package com.konkuk.ma.domain.member.entity

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.PhoneNumber
import com.konkuk.ma.domain.member.domain.Region
import java.time.LocalDate
import java.time.LocalDateTime

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

    val university: String?,

    val withdrawalRequestedAt: LocalDateTime? = null
) {
    fun toDomain(): Member {
        return Member(
            id = id,
            email = Email(email),
            password = password,
            nickname = nickname,
            gender = gender,
            phoneNumber = PhoneNumber(phoneNumber),
            name = name,
            region = region,
            birthDate = birthDate,
            highSchool = highSchool,
            university = university,
            withdrawalRequestedAt = withdrawalRequestedAt
        )
    }

    companion object {
        fun from(member: Member): MemberEntity = MemberEntity(
            id = member.id,
            email = member.email.value,
            password = member.password,
            nickname = member.nickname,
            gender = member.gender,
            phoneNumber = member.phoneNumber.fullNumber,
            name = member.name,
            region = member.region,
            birthDate = member.birthDate,
            highSchool = member.highSchool,
            university = member.university,
            withdrawalRequestedAt = member.withdrawalRequestedAt
        )
    }
}
