package com.konkuk.ma.domain.member.entity

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
)
