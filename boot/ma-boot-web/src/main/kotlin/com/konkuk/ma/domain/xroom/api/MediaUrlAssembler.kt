package com.konkuk.ma.domain.xroom.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class MediaUrlAssembler(
    @Value("\${file.upload.url-prefix:/files}")
    private val urlPrefix: String,
) {
    fun toUrl(storageKey: String): String = "$urlPrefix/$storageKey"

    fun toUrlOrNull(storageKey: String?): String? = storageKey?.let { toUrl(it) }
}
