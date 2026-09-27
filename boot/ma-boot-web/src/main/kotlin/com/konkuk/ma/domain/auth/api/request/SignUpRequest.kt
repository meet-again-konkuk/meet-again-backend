package com.konkuk.ma.domain.auth.api.request

import com.konkuk.ma.support.validation.ValidationMessages
import com.konkuk.ma.support.validation.ValidationPatterns
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.domain.Region
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.time.LocalDate

class SignUpRequest(
    @field:NotBlank(message = ValidationMessages.EMAIL_REQUIRED)
    @field:Email(message = ValidationMessages.EMAIL_INVALID)
    val email: String,

    @field:NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
    @field:Pattern(regexp = ValidationPatterns.PASSWORD, message = ValidationMessages.PASSWORD_INVALID)
    val password: String,

    @field:NotBlank(message = ValidationMessages.PHONE_NUMBER_REQUIRED)
    @field:Pattern(regexp = ValidationPatterns.PHONE_NUMBER, message = ValidationMessages.PHONE_NUMBER_INVALID)
    val phoneNumber: String,

    @field:Pattern(regexp = ValidationPatterns.NICKNAME, message = ValidationMessages.NICKNAME_INVALID)
    val nickname: String,

    @field:NotBlank(message = ValidationMessages.NAME_REQUIRED)
    @field:Pattern(regexp = ValidationPatterns.NAME, message = ValidationMessages.NAME_INVALID)
    val name: String,

    val gender: Gender,

    @field:NotNull(message = ValidationMessages.BIRTH_DATE_REQUIRED)
    val birthDate: LocalDate,

    @field:NotNull(message = ValidationMessages.REGION_REQUIRED)
    val region: Region,

    val highSchool: String?,

    val university: String?,
) {
    fun toNewMember(): NewMember {
        return NewMember.of(
            email = this.email,
            password = this.password,
            nickname = this.nickname,
            gender = this.gender,
            phoneNumber = this.phoneNumber,
            name = this.name,
            birthDate = this.birthDate,
            region = this.region,
            highSchool = this.highSchool,
            university = this.university
        )
    }
}
