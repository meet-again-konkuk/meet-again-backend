package com.konkuk.ma.support.payload.response

enum class ErrorCode(val status: Int, val message: String) {
    // 400
    INVALID_INPUT_VALUE(400, "Invalid Input Value"),
    INVALID_TYPE_VALUE(400, "Invalid Type Value"),
    LOGIN_INPUT_INVALID(400, "Login input is invalid"),
    EXPIRED_TOKEN(400, "Token is expired"),
    MALFORMED_TOKEN(400, "Token is Malformed"),
    INVALID_TOKEN(400, "Invalid Request"),
    OTHER_TOKEN_ERROR(400, "Unexpected JWT Error"),
    FILE_SIZE_EXCEEDED(400, "File size exceeded"),

    // 401
    UNAUTHORIZED(401, "Unauthorized"),

    // 402
    PAYMENT_APPROVAL_FAILED(402, "Payment Approval Failed"),

    // 403
    ACCESS_DENIED(403, "Access is Denied"),

    // 404
    ENTITY_NOT_FOUND(404, "Entity Not Found"),

    // 409
    ENTITY_DUPLICATION(409, "Entity Duplication"),

    // 410
    WITHDRAWAL_EXPIRED(410, "Withdrawal grace period has expired"),

    // 500
    INTERNAL_SERVER_ERROR(500, "Server Error"),
}
