package com.konkuk.ma.support.payload.response

import com.konkuk.ma.domain.common.domain.page.CursorResult

class CursorResponse<T>(
    val data: T,
    val hasNext: Boolean,
    val nextCursorId: Long?,
) {
    companion object {
        fun <D, T> from(cursorResult: CursorResult<D>, transform: (D) -> T): CursorResponse<T> {
            return CursorResponse(
                data = transform(cursorResult.data),
                hasNext = cursorResult.hasNext,
                nextCursorId = cursorResult.nextCursorId,
            )
        }
    }
}
