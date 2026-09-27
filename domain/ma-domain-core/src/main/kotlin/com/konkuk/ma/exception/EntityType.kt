package com.konkuk.ma.exception

enum class EntityType(val entityName: String, val keyName: String) {
    MEMBER("Member", "email"),
    MATCHING_RESULT("MatchingResult", "id"),
    REFRESH_TOKEN("RefreshToken", "memberId"),
    COMMUNITY_POST("CommunityPost", "id"),
    COMMUNITY_COMMENT("CommunityComment", "id"),
    COMMUNITY_REPORT("CommunityReport", "id"),
    COMMUNITY_BLOCK("CommunityBlock", "id"),
    TARGET_INFO("TargetInfo", "id"),
    XROOM("Xroom", "id"),
    MEMORY("Memory", "id"),
    MEDIA("Media", "id"),
    POINT_PRODUCT("PointProduct", "id"),
    POINT_HISTORY("PointHistory", "idempotencyKey"),
    MEMBER_POINT("MemberPoint", "email"),
    NOTIFICATION("Notification", "id"),
}
