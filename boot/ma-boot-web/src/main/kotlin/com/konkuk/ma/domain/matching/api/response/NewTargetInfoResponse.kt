package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.support.id.EncryptId

class NewTargetInfoResponse(
    @EncryptId(ObfuscationType.TARGET_INFO)
    val targetInfoId: Long,
    val registerEmail: String
)
