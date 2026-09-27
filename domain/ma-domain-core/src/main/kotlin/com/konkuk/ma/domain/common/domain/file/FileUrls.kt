package com.konkuk.ma.domain.common.domain.file

class FileUrls(
    val data: Map<Long, String>,
) {
    fun urlOf(ownerId: Long): String? = data[ownerId]
}
