package com.konkuk.ma.domain.common.domain.page

data class CursorIdCondition(
    val cursorId: Long?,
    val size: Int,
) {
    companion object {
        private const val DEFAULT_SIZE = 20

        fun of(cursorId: Long?, size: Int? = null): CursorIdCondition {
            return CursorIdCondition(cursorId = cursorId, size = size ?: DEFAULT_SIZE)
        }
    }
}
