package com.konkuk.ma.domain.xroom.domain.media

class Media(
    val id: Long,
    val memoryId: Long,
    val storageKey: String,
    val originalFilename: String,
    val mimeType: String,
    val fileSize: Long,
    val thumbnailKey: String?,
) {
    fun belongsTo(memoryId: Long): Boolean = this.memoryId == memoryId

    fun toPhotoUrl(baseUrl: String): String = "$baseUrl/$storageKey"

    fun toThumbnailUrl(baseUrl: String): String? = thumbnailKey?.let { "$baseUrl/$it" }
}
