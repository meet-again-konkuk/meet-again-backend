package com.konkuk.ma.domain.common.domain

import com.konkuk.ma.exception.InvalidValueException

data class Email(val value: String) {
    init {
        if (value.isBlank()) throw InvalidValueException(Email::class, value, "이메일은 비어있을 수 없습니다.")
        if (!EMAIL_REGEX.matches(value)) throw InvalidValueException(Email::class, value, "유효하지 않은 이메일 형식입니다.")
    }

    override fun toString(): String = value

    fun masked(): String {
        val localPart = value.substringBefore(AT_SIGN)
        val domain = value.substringAfter(AT_SIGN)
        val visibleLength = if (localPart.length >= FULL_PREFIX_THRESHOLD) VISIBLE_PREFIX_LENGTH else MINIMUM_VISIBLE_LENGTH
        return "${localPart.take(visibleLength)}$MASK$AT_SIGN$domain"
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val WITHDRAWN_FORMAT = "withdrawn_%d@deleted.local"
        private const val AT_SIGN = "@"
        private const val MASK = "***"
        private const val FULL_PREFIX_THRESHOLD = 4
        private const val VISIBLE_PREFIX_LENGTH = 3
        private const val MINIMUM_VISIBLE_LENGTH = 1

        fun withdrawn(memberId: Long): Email = Email(WITHDRAWN_FORMAT.format(memberId))
    }
}
