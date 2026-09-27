package com.konkuk.ma.domain.auth.api.request

import com.konkuk.ma.support.validation.ValidationMessages
import com.konkuk.ma.support.validation.ValidationPatterns
import jakarta.validation.constraints.Pattern

class SmsVerificationConfirmRequest(
    @field:Pattern(regexp = ValidationPatterns.PHONE_NUMBER_11, message = ValidationMessages.PHONE_NUMBER_INVALID)
    val phoneNumber: String,

    @field:Pattern(regexp = ValidationPatterns.VERIFICATION_CODE, message = ValidationMessages.VERIFICATION_CODE_INVALID)
    val verificationCode: String
)
