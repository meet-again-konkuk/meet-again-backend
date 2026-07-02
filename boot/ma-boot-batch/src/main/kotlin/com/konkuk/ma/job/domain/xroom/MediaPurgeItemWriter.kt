package com.konkuk.ma.job.domain.xroom

import com.konkuk.ma.domain.xroom.domain.media.Media
import com.konkuk.ma.domain.xroom.domain.media.MediaPurger
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter

class MediaPurgeItemWriter(
    private val mediaPurger: MediaPurger,
) : ItemWriter<Media> {

    override fun write(chunk: Chunk<out Media>) {
        chunk.items.forEach(mediaPurger::purge)
    }
}
