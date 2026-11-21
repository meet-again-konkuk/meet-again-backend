package com.konkuk.ma.support.payload.response

enum class ErrorCode(val status: Int, val message: String) {
    INVALID_INPUT_VALUE(400, "Invalid Input Value"),
    INVALID_TYPE_VALUE(400, "Invalid Type Value"),
    LOGIN_INPUT_INVALID(400, "Login input is invalid"),
    EXPIRED_TOKEN(400, "Token is expired"),
    MALFORMED_TOKEN(400, "Token is Malformed"),
    INVALID_TOKEN(400, "Invalid Request"),
    OTHER_TOKEN_ERROR(400, "Unexpected JWT Error"),
    FILE_SIZE_EXCEEDED(400, "File size exceeded"),

    UNAUTHORIZED(401, "Unauthorized"),

    PAYMENT_APPROVAL_FAILED(402, "Payment Approval Failed"),

    ACCESS_DENIED(403, "Access is Denied"),

    ENTITY_NOT_FOUND(404, "Entity Not Found"),

    ENTITY_DUPLICATION(409, "Entity Duplication"),
    WITHDRAWAL_PENDING(409, "Withdrawal request is pending"),

    INTERNAL_SERVER_ERROR(500, "Server Error"),
}
