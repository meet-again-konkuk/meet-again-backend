package com.konkuk.ma.domain.xroom.domain.media

class Medias(
    val data: List<Media>,
) {
    fun findByMemory(memoryId: Long): Media? = data.firstOrNull { it.belongsTo(memoryId) }
}
