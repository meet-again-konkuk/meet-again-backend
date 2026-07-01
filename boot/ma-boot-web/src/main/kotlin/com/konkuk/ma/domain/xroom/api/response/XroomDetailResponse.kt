package com.konkuk.ma.domain.xroom.api.response

import com.konkuk.ma.config.WebConfig.Companion.FILE_URL_PREFIX
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
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
        fun from(xroomDetail: XroomDetail): XroomDetailResponse {
            return XroomDetailResponse(
                id = xroomDetail.id,
                title = xroomDetail.title,
                recipientName = xroomDetail.recipientName,
                template = xroomDetail.template,
                finalMessage = xroomDetail.finalMessage,
                memories = xroomDetail.memories.map { memory ->
                    MemoryDetailResponse.from(memory, xroomDetail.photoUrlOf(memory.id, FILE_URL_PREFIX))
                },
            )
        }
    }
}
