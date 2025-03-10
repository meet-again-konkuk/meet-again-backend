package com.konkuk.ma.auth.domain

class SmsVerification(
    val phoneNumber: String,
    val verificationCode: Int
)
