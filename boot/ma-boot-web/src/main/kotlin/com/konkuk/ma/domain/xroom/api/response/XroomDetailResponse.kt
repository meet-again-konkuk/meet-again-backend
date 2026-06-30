package com.konkuk.ma.domain.xroom.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.xroom.api.MediaUrlAssembler
import com.konkuk.ma.domain.xroom.domain.XroomDetail
import com.konkuk.ma.support.id.EncryptId

class XroomDetailResponse(
    @EncryptId(ObfuscationType.XROOM)
    val id: Long,
    val title: String,
    val recipientName: String,
    val template: String,
    val finalMessage: String?,
    val memories: List<MemoryDetailResponse>,
) {
    companion object {
        fun from(xroomDetail: XroomDetail, mediaUrlAssembler: MediaUrlAssembler): XroomDetailResponse {
            return XroomDetailResponse(
                id = xroomDetail.id,
                title = xroomDetail.title,
                recipientName = xroomDetail.recipientName,
                template = xroomDetail.template,
                finalMessage = xroomDetail.finalMessage,
                memories = xroomDetail.memories.map { memory ->
                    val photoUrl = mediaUrlAssembler.toUrlOrNull(xroomDetail.storageKeyOf(memory.id))
                    MemoryDetailResponse.from(memory, photoUrl)
                },
            )
        }
    }
}
