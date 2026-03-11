package com.konkuk.ma.domain.member.domain

data class FourDigit(val value: String) {
    init {
        require(value.length == 4) { "FourDigit는 4자리여야 합니다. value=$value" }
        require(value.all { it in '0'..'9' }) { "FourDigit는 0~9 숫자만 허용됩니다. value=$value" }
    }

    override fun toString(): String = value
}
