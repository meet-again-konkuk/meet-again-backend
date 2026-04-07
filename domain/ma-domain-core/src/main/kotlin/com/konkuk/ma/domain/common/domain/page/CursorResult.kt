package com.konkuk.ma.domain.common.domain.page

class CursorResult<T>(
    val data: T,
    val hasNext: Boolean,
    val nextCursorId: Long?,
) {
    companion object {
        fun <T> of(allData: List<T>, size: Int, cursorIdExtractor: (T) -> Long): CursorResult<List<T>> {
            val hasNext = allData.size > size
            val sliced = if (hasNext) allData.subList(0, size) else allData
            val nextCursorId = if (hasNext) cursorIdExtractor(sliced.last()) else null

            return CursorResult(
                data = sliced,
                hasNext = hasNext,
                nextCursorId = nextCursorId,
            )
        }
    }
}
