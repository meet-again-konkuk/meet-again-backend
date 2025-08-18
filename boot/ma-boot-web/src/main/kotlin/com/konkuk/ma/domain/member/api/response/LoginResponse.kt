package com.konkuk.ma.domain.member.api.response

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val email: String,
    val nickname: String
) 