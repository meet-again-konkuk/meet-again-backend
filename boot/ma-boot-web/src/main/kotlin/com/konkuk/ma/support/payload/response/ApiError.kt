package com.konkuk.ma.support.payload.response

import java.time.LocalDateTime

class ApiError(
    val code: ErrorCode,
    val message: String = code.message,
) {
    val timestamp: LocalDateTime = LocalDateTime.now()
}
