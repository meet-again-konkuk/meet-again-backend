package com.konkuk.ma.domain.auth.domain

class LoginInfo(
    val email: String,

    val nickname: String,

    val accessToken: String,

    val refreshToken: RefreshToken
)
