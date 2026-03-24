package com.konkuk.ma.domain.common.domain.file

class PhotoFile(
    val originalFileName: String,
    val extension: AllowedExtension,
    val sizeInBytes: Long,
    val content: ByteArray
) {
    init {
        require(sizeInBytes <= MAX_FILE_SIZE_BYTES) {
            "파일 크기는 ${MAX_FILE_SIZE_MB}MB를 초과할 수 없습니다: ${sizeInBytes}bytes"
        }
        require(content.isNotEmpty()) {
            "파일 내용이 비어있습니다."
        }
    }

    companion object {
        private const val MAX_FILE_SIZE_MB = 10
        private const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024L * 1024L

        fun create(originalFileName: String?, sizeInBytes: Long, content: ByteArray): PhotoFile {
            val fileName = originalFileName ?: "unknown"
            return PhotoFile(
                originalFileName = fileName,
                extension = AllowedExtension.from(fileName),
                sizeInBytes = sizeInBytes,
                content = content
            )
        }
    }
}
