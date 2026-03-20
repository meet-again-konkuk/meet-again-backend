package com.konkuk.ma.domain.auth.api.response

class SignUpResponse(
    val memberId: Long,
    val email: String,
    val nickname: String,
    val message: String = "회원가입이 완료되었습니다."
)
