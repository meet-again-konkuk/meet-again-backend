package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.common.exception.InvalidValueException

data class FourDigit(val value: String) {
    init {
        if (value.length != 4) throw InvalidValueException(FourDigit::class, value, "4자리여야 합니다.")
        if (!value.all { it in '0'..'9' }) throw InvalidValueException(FourDigit::class, value, "0~9 숫자만 허용됩니다.")
    }

    override fun toString(): String = value
}
