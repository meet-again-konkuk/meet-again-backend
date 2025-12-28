package com.konkuk.ma.domain.member.domain

import java.time.LocalDate

class NewMember(
    val email: String,
    val password: String,
    val nickname: String,
    val gender: Gender,
    val phoneNumber: PhoneNumber,
    val name: String,
    val birthDate: LocalDate,
    val region: Region,
    val highSchool: String?,
    val university: String?
)
