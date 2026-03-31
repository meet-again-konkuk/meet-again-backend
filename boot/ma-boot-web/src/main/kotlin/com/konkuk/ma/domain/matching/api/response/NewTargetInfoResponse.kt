package com.konkuk.ma.domain.matching.api.response

import com.konkuk.ma.support.id.EncryptId

class NewTargetInfoResponse(
    @EncryptId
    val targetInfoId: Long,
    val registerEmail: String
)
