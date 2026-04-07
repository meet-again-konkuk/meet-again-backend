package com.konkuk.ma.domain.common.domain.page

data class CursorRequest(
    val cursorId: Long?,
    val size: Int,
) {
    val fetchSize: Int get() = size + 1

    companion object {
        private const val DEFAULT_SIZE = 20

        fun of(cursorId: Long?, size: Int? = null): CursorRequest {
            return CursorRequest(cursorId = cursorId, size = size ?: DEFAULT_SIZE)
        }
    }
}
