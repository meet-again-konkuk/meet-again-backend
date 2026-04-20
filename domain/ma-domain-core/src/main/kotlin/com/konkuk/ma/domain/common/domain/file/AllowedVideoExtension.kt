package com.konkuk.ma.domain.common.domain.file

import com.konkuk.ma.exception.InvalidValueException

class AllowedVideoExtension private constructor(val normalized: String) {

    companion object {
        private val ALLOWED_EXTENSIONS = setOf("mp4", "mov", "webm")
        private val CACHE = ALLOWED_EXTENSIONS.associateWith { AllowedVideoExtension(it) }

        fun from(originalFileName: String): AllowedVideoExtension {
            val extension = originalFileName.substringAfterLast('.', "")
                .lowercase()
            return CACHE[extension]
                ?: throw InvalidValueException(
                    AllowedVideoExtension::class,
                    extension,
                    "허용되지 않는 확장자 '$extension'. 허용 확장자: ${ALLOWED_EXTENSIONS.joinToString()}"
                )
        }
    }
}
