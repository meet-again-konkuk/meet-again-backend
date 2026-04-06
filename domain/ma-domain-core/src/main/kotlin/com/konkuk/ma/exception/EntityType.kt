package com.konkuk.ma.exception

enum class EntityType(val entityName: String) {
    MEMBER("Member"),
    MATCHING_RESULT("MatchingResult"),
    REFRESH_TOKEN("RefreshToken"),
}
