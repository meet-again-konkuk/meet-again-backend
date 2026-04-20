package com.konkuk.ma.domain.common.domain.id

enum class ObfuscationType(val saltSuffix: String) {
    MEMBER("member"),
    TARGET_INFO("target-info"),
    MEMBER_PHOTO("member-photo"),
    MATCHING_RESULT("matching-result"),
    XROOM("xroom"),
    XROOM_BLOCK("xroom-block"),
    XROOM_BLOCK_PHOTO("xroom-block-photo"),
    XROOM_BLOCK_VIDEO("xroom-block-video"),
}
