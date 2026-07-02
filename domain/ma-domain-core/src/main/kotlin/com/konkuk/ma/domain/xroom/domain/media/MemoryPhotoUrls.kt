package com.konkuk.ma.domain.xroom.domain.media

class MemoryPhotoUrls(
    private val data: Map<Long, String>,
) {
    fun photoUrlOf(memoryId: Long): String? = data[memoryId]
}
