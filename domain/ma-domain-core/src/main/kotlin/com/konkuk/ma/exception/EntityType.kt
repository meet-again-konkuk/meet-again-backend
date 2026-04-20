package com.konkuk.ma.exception

enum class EntityType(val entityName: String, val keyName: String) {
    MEMBER("Member", "email"),
    MATCHING_RESULT("MatchingResult", "id"),
    REFRESH_TOKEN("RefreshToken", "email"),
    COMMUNITY_POST("CommunityPost", "id"),
    COMMUNITY_COMMENT("CommunityComment", "id"),
    TARGET_INFO("TargetInfo", "id"),
    XROOM("Xroom", "id"),
    XROOM_BLOCK("XroomBlock", "id"),
    XROOM_BLOCK_PHOTO("XroomBlockPhoto", "id"),
    XROOM_BLOCK_VIDEO("XroomBlockVideo", "id"),
}
