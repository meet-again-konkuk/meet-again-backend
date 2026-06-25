package com.konkuk.ma.domain.xroom.domain

class XroomTitle(val value: String) {

    init {
        require(value.isNotBlank()) { "방 제목은 비어 있을 수 없습니다." }
        require(value.length <= MAX_LENGTH) { "방 제목은 ${MAX_LENGTH}자 이하여야 합니다." }
    }

    companion object {
        private const val MAX_LENGTH = 100
        private const val DEFAULT_VALUE = "기억의 방"

        val DEFAULT = XroomTitle(DEFAULT_VALUE)
    }
}
