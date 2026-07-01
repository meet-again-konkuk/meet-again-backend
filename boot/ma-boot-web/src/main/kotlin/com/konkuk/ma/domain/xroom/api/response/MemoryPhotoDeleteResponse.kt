package com.konkuk.ma.domain.xroom.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.support.id.EncryptId

class MemoryPhotoDeleteResponse(
    @EncryptId(ObfuscationType.MEMORY)
    val memoryId: Long,
    val photoDeleted: Boolean = true,
)
