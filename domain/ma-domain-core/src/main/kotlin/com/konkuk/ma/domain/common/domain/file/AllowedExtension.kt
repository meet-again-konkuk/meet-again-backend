package com.konkuk.ma.domain.common.domain.file

import com.konkuk.ma.exception.InvalidValueException

class AllowedExtension private constructor(val normalized: String, val mimeType: String) {

    companion object {
        private val ALLOWED_EXTENSIONS = mapOf(
            "jpeg" to "image/jpeg",
            "jpg" to "image/jpeg",
            "png" to "image/png",
            "svg" to "image/svg+xml",
            "webp" to "image/webp",
        )
        private val CACHE = ALLOWED_EXTENSIONS.mapValues { (extension, mimeType) -> AllowedExtension(extension, mimeType) }

        fun from(originalFileName: String): AllowedExtension {
            val extension = originalFileName.substringAfterLast('.', "")
                .lowercase()
            return CACHE[extension]
                ?: throw InvalidValueException(
                    AllowedExtension::class,
                    extension,
                    "허용: ${ALLOWED_EXTENSIONS.keys.joinToString()}"
                )
        }
    }
}
