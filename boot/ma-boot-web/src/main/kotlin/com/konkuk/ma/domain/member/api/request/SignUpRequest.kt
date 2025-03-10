package com.konkuk.ma.domain.member.api.request

class SignUpRequest(
    val email: String,

    val password: String,

    val nickname: String,

    val cellPhoneNumber: String
)
