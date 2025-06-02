package com.konkuk.ma.auth.domain

class SmsVerification(
    val phoneNumber: String,
    val verificationCode: Int
) {
    fun isVerified(generatedVerificationCode: Int?): Boolean {
        return verificationCode == generatedVerificationCode
    }
}
