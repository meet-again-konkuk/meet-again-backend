package com.konkuk.ma.domain.xroom.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.xroom.domain.MyXroom
import com.konkuk.ma.support.id.EncryptId
import java.time.LocalDateTime

class MyXroomResponse(
    @EncryptId(ObfuscationType.XROOM)
    val id: Long,
    val title: String,
    val recipientName: String,
    @EncryptId(ObfuscationType.TARGET_INFO)
    val targetInfoId: Long,
    val memoryCount: Int,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(myXroom: MyXroom): MyXroomResponse {
            return MyXroomResponse(
                id = myXroom.id,
                title = myXroom.title,
                recipientName = myXroom.recipientName,
                targetInfoId = myXroom.targetInfoId,
                memoryCount = myXroom.memoryCount,
                updatedAt = myXroom.updatedAt,
            )
        }
    }
}
