package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.exception.InvalidValueException

data class VerificationCode(val value: String) {
    init {
        if (!PATTERN.matches(value)) {
            throw InvalidValueException(VerificationCode::class, value, "6자리 숫자여야 합니다.")
        }
    }

    override fun toString(): String = value

    companion object {
        private val PATTERN = Regex("^\\d{6}$")
        private const val MAX_VALUE = 999_999

        fun random(): VerificationCode =
            VerificationCode((0..MAX_VALUE).random().toString().padStart(6, '0'))
    }
}
