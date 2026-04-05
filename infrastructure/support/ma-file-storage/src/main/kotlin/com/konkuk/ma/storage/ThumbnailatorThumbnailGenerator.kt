package com.konkuk.ma.storage

import com.konkuk.ma.domain.common.domain.file.port.ThumbnailGenerator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import net.coobird.thumbnailator.Thumbnails
import org.springframework.stereotype.Component

@Component
class ThumbnailatorThumbnailGenerator : ThumbnailGenerator {

    override fun generate(source: ByteArray, width: Int): ByteArray {
        val outputStream = ByteArrayOutputStream()
        Thumbnails.of(ByteArrayInputStream(source))
            .size(width, width)
            .keepAspectRatio(true)
            .toOutputStream(outputStream)
        return outputStream.toByteArray()
    }
}
