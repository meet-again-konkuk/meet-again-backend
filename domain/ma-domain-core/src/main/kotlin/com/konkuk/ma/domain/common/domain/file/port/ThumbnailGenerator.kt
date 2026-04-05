package com.konkuk.ma.domain.common.domain.file.port

interface ThumbnailGenerator {
    fun generate(source: ByteArray, width: Int): ByteArray
}
