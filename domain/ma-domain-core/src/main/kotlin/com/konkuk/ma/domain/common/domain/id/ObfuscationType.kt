package com.konkuk.ma.domain.common.domain.id

enum class ObfuscationType(val saltSuffix: String) {
    MEMBER("member"),
    TARGET_INFO("target-info"),
    MEMBER_PHOTO("member-photo"),
    MATCHING_RESULT("matching-result"),
    XROOM("xroom"),
    POINT_PRODUCT("point-product"),
    POINT_TRANSACTION("point-transaction"),
}
