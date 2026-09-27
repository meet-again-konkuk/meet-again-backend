package com.konkuk.ma.domain.auth.domain

class SmsVerification(
    val phoneNumber: String,
    val verificationCode: VerificationCode
)
