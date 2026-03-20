package com.konkuk.ma.domain.auth.api.request

import com.konkuk.ma.domain.auth.application.command.SignUpCommand
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.time.LocalDate

class SignUpRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "유효하지 않은 이메일 형식입니다.")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$",
        message = "비밀번호는 영문/숫자 조합 8자리 이상 16자리 이하여야 합니다."
    )
    val password: String,

    @field:NotBlank(message = "휴대폰 번호는 필수입니다.")
    @field:Pattern(
        regexp = "^010\\d{7,8}$",
        message = "유효하지 않은 휴대폰 번호 형식입니다. (예: 01012345678)"
    )
    val phoneNumber: String,

    @field:Pattern(regexp = "^[a-zA-Z가-힣]{2,8}$", message = "유효하지 않은 닉네임 형식입니다.")
    val nickname: String,

    @field:NotBlank(message = "이름은 필수입니다.")
    @field:Pattern(regexp = "^[가-힣]{2,10}$", message = "이름은 한글 2자 이상 10자 이하여야 합니다.")
    val name: String,

    val gender: Gender,

    @field:NotNull(message = "생년월일은 필수입니다.")
    val birthDate: LocalDate,

    @field:NotNull(message = "지역은 필수입니다.")
    val region: Region,

    val highSchool: String?,

    val university: String?,
) {
    fun toCommand(): SignUpCommand {
        return SignUpCommand(
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
