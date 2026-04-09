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
)
