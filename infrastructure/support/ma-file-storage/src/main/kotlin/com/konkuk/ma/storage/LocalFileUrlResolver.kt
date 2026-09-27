package com.konkuk.ma.storage

import com.konkuk.ma.domain.common.domain.file.port.FileUrlResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["file.storage.mode"], havingValue = "local", matchIfMissing = true)
class LocalFileUrlResolver(
    @Value("\${file.upload.url-prefix:/files}")
    private val urlPrefix: String,
) : FileUrlResolver {

    override fun resolve(storageKey: String): String = "$urlPrefix/$storageKey"
}
