package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.common.domain.Email

class LoginInfo(
    val email: Email,

    val nickname: String,

    val accessToken: String,

    val refreshToken: RefreshToken
)
