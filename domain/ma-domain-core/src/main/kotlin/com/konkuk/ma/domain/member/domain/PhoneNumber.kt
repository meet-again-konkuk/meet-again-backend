package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.exception.InvalidValueException

class PhoneNumber(
    phoneNumber: String,
) {
    val firstNumber: String
    val middleNumber: FourDigit
    val lastNumber: FourDigit
    val formatted: String get() = "$firstNumber-$middleNumber-$lastNumber"
    val fullNumber: String get() = "$firstNumber$middleNumber$lastNumber"

    fun masked(): String = "$firstNumber-$MASKED_MIDDLE-$lastNumber"

    init {
        val normalized = phoneNumber.filterNot { it == '-' || it.isWhitespace() }
        if (normalized.length < 10) throw InvalidValueException(PhoneNumber::class, phoneNumber, "전화번호는 최소 10자리여야 합니다.")
        if (!normalized.startsWith(ALLOWED_PREFIX)) throw InvalidValueException(PhoneNumber::class, phoneNumber, "앞자리는 010만 허용됩니다.")

        firstNumber = normalized.take(3)
        lastNumber = FourDigit(normalized.takeLast(4))
        middleNumber = FourDigit(normalized.substring(3, normalized.length - 4))
    }

    companion object {
        private const val ALLOWED_PREFIX = "010"
        private const val MASKED_MIDDLE = "****"
    }
}
