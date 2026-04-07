package com.konkuk.ma.support.payload.response

class CursorResponse<T>(
    val data: T,
    val hasNext: Boolean,
    val nextCursorId: Long?,
)
