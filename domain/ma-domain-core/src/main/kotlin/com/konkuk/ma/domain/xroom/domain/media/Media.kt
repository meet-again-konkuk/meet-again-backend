package com.konkuk.ma.domain.xroom.domain.media

import java.time.LocalDateTime

class Media(
    val id: Long,
    val memoryId: Long,
    val storageKey: String,
    val originalFilename: String,
    val mimeType: String,
    val fileSize: Long,
    val thumbnailKey: String?,
    val createdDate: LocalDateTime,
) {
    fun belongsTo(memoryId: Long): Boolean = this.memoryId == memoryId
}
