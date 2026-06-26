package com.konkuk.ma.domain.xroom.api.response

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
    val memories: List<Any>,
) {
    companion object {
        fun from(xroomDetail: XroomDetail): XroomDetailResponse {
            return XroomDetailResponse(
                id = xroomDetail.id,
                title = xroomDetail.title,
                recipientName = xroomDetail.recipientName,
                template = xroomDetail.template,
                finalMessage = xroomDetail.finalMessage,
                // 기억(Memory)은 Phase 2에서 채움 — Phase 1에서는 항상 빈 배열
                memories = emptyList(),
            )
        }
    }
}
