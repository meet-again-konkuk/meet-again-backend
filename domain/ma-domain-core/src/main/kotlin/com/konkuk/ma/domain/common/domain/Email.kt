package com.konkuk.ma.domain.common.domain

data class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "이메일은 비어있을 수 없습니다." }
        require(EMAIL_REGEX.matches(value)) { "유효하지 않은 이메일 형식입니다: $value" }
    }

    override fun toString(): String = value

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
