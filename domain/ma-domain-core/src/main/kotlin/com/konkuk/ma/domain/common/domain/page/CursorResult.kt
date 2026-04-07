package com.konkuk.ma.domain.common.domain.page

class CursorResult<T>(
    val data: T,
    val hasNext: Boolean,
    val nextCursorId: Long?,
) {
    companion object {
        fun <T> of(data: List<T>, size: Int, cursorIdExtractor: (T) -> Long): CursorResult<List<T>> {
            val hasNext = data.size >= size
            val nextCursorId = if (hasNext && data.isNotEmpty()) cursorIdExtractor(data.last()) else null

            return CursorResult(
                data = data,
                hasNext = hasNext,
                nextCursorId = nextCursorId,
            )
        }
    }
}
