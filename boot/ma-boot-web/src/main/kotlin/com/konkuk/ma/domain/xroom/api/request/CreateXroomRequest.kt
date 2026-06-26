package com.konkuk.ma.domain.xroom.api.request

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.support.id.EncryptId

class CreateXroomRequest(
    @field:EncryptId(ObfuscationType.TARGET_INFO)
    val targetInfoId: Long,
    val finalMessage: String? = null,
)
