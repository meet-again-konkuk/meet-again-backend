package com.konkuk.ma.domain.auth.api.response

class SmsVerificationConfirmResponse(
    val phoneNumber: String,

    val verificationCode: String,

    val verified: Boolean
)
