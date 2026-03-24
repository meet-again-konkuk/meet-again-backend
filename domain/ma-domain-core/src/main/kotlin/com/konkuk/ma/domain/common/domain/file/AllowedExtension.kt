package com.konkuk.ma.domain.common.domain.file

import com.konkuk.ma.domain.common.exception.InvalidValueException

class AllowedExtension private constructor(val normalized: String) {

    companion object {
        private val ALLOWED_EXTENSIONS = setOf("jpeg", "jpg", "png", "svg", "webp")
        private val CACHE = ALLOWED_EXTENSIONS.associateWith { AllowedExtension(it) }

        fun from(originalFileName: String): AllowedExtension {
            val extension = originalFileName.substringAfterLast('.', "")
                .lowercase()
            return CACHE[extension]
                ?: throw InvalidValueException(
                    AllowedExtension::class,
                    extension,
                    "허용: ${ALLOWED_EXTENSIONS.joinToString()}"
                )
        }
    }
}
