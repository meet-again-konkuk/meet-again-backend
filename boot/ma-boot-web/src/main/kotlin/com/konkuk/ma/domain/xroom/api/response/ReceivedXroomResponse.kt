package com.konkuk.ma.domain.xroom.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.xroom.domain.ReceivedXroom
import com.konkuk.ma.support.id.EncryptId

class ReceivedXroomResponse(
    @EncryptId(ObfuscationType.XROOM)
    val id: Long,
    val title: String,
    val senderName: String,
    val memoryCount: Int,
) {
    companion object {
        fun from(receivedXroom: ReceivedXroom): ReceivedXroomResponse {
            return ReceivedXroomResponse(
                id = receivedXroom.id,
                title = receivedXroom.title,
                senderName = receivedXroom.senderName,
                memoryCount = receivedXroom.memoryCount,
            )
        }
    }
}
