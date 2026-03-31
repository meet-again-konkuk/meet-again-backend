package com.konkuk.ma.domain.auth.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.support.id.EncryptId

class SignUpResponse(
    @EncryptId(ObfuscationType.MEMBER)
    val memberId: Long,
    val email: String,
    val nickname: String,
    val message: String = "회원가입이 완료되었습니다."
)
