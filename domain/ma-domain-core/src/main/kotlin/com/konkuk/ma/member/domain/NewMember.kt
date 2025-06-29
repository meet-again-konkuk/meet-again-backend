package com.konkuk.ma.member.domain

import java.time.LocalDate

class NewMember(
    val email: String,
    val password: String,
    val nickname: String,
    val phoneNumber: String,
    val name: String,
    val birthDate: LocalDate,
    val highSchool: String?,
    val university: String?
)
