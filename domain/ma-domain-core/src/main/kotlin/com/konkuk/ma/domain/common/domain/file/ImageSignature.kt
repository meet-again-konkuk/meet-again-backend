package com.konkuk.ma.domain.common.domain.file

enum class ImageSignature(
    val extensions: Set<String>,
    val magicBytes: List<Int>,
) {
    JPEG(setOf("jpg", "jpeg"), listOf(0xFF, 0xD8, 0xFF)),
    PNG(setOf("png"), listOf(0x89, 0x50, 0x4E, 0x47)),
    WEBP(setOf("webp"), listOf(0x52, 0x49, 0x46, 0x46)),
    ;

    companion object {
        fun matches(header: ByteArray, extension: String): Boolean {
            val signature = entries.firstOrNull { extension in it.extensions } ?: return false
            if (header.size < signature.magicBytes.size) return false
            return signature.magicBytes.withIndex().all { (index, byte) -> header[index] == byte.toByte() }
        }
    }
}
