package com.konkuk.ma.exception

enum class EntityType(val entityName: String, val keyName: String) {
    MEMBER("Member", "email"),
    MATCHING_RESULT("MatchingResult", "id"),
    REFRESH_TOKEN("RefreshToken", "email"),
    COMMUNITY_POST("CommunityPost", "id"),
    COMMUNITY_COMMENT("CommunityComment", "id"),
    TARGET_INFO("TargetInfo", "id"),
    XROOM("Xroom", "id"),
    POINT_PRODUCT("PointProduct", "id"),
    POINT_TRANSACTION("PointTransaction", "idempotencyKey"),
    MEMBER_POINT("MemberPoint", "email"),
}
