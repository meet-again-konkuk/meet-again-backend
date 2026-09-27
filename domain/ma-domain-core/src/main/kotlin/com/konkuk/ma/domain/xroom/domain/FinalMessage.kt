package com.konkuk.ma.domain.xroom.domain

class FinalMessage(val value: String) {

    init {
        require(value.isNotBlank()) { "마지막 메시지는 비어 있을 수 없습니다." }
        require(value.length <= MAX_LENGTH) { "마지막 메시지는 ${MAX_LENGTH}자 이하여야 합니다." }
    }

    companion object {
        private const val MAX_LENGTH = 1000

        fun of(value: String?): FinalMessage? {
            if (value.isNullOrBlank()) return null
            return FinalMessage(value)
        }
    }
}
