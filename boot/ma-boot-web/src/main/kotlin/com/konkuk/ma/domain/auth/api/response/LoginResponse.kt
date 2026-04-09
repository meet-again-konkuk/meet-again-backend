package com.konkuk.ma.domain.auth.api.response

import com.konkuk.ma.domain.auth.domain.LoginInfo

class LoginResponse(
    val email: String,

    val nickname: String,

    val accessToken: String,

    val refreshToken: String
) {
    constructor(loginInfo: LoginInfo) : this(
        email = loginInfo.email.value,
        nickname = loginInfo.nickname,
        accessToken = loginInfo.accessToken,
        refreshToken = loginInfo.refreshToken.token
    )
}
