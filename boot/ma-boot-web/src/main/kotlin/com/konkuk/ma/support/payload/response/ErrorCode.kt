package com.konkuk.ma.support.payload.response

enum class ErrorCode(val status: Int, val message: String) {
    INVALID_INPUT_VALUE(400,"Invalid Input Value"),
    ENTITY_NOT_FOUND(400, "Entity Not Found"),
    ENTITY_DUPLICATION(400, "Entity Duplication"),
    INVALID_TYPE_VALUE(400, "Invalid Type Value"),

    HANDLE_ACCESS_DENIED(403, "Access is Denied"),
    METHOD_NOT_ALLOWED(405, "Invalid Input Value"),
    INTERNAL_SERVER_ERROR(500, "Server Error"),
    INVALID_STATE_ERROR(500, "Invalid State Error"),

    // Member
    EMAIL_DUPLICATION(400,"Email is Duplication"),
    LOGIN_INPUT_INVALID(400,"Login input is invalid"),

    EXPIRED_TOKEN(400, "Token is expired"),
    MALFORMED_TOKEN(400, "Token is Malformed"),
    INVALID_TOKEN(400, "Invalid Request"),
    OTHER_TOKEN_ERROR(400, "Unexpected JWT Error"),
}
