package com.konkuk.ma.domain.member.api.request

import com.konkuk.ma.member.application.command.NewMemberCommand

fun SignUpRequest.toCommand(): NewMemberCommand {
    return NewMemberCommand(
        email = this.email,
        password = this.password,
        nickname = this.nickname,
        phoneNumber = this.phoneNumber,
        name = this.name,
        birthDate = this.birthDate,
        highSchool = this.highSchool,
        university = this.university
    )
} 