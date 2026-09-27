package com.konkuk.ma.job.domain.xroom

import com.konkuk.ma.domain.xroom.domain.media.Media
import com.konkuk.ma.domain.xroom.domain.port.MediaQueryRepository
import java.time.LocalDateTime
import org.springframework.batch.item.ItemReader

open class DeletedMediaItemReader(
    private val mediaQueryRepository: MediaQueryRepository,
    private val purgeCutoff: LocalDateTime,
    private val pageSize: Int,
) : ItemReader<Media> {

    private val buffer: ArrayDeque<Media> = ArrayDeque()
    private var cursorId: Long? = null
    private var exhausted: Boolean = false

    override fun read(): Media? {
        if (buffer.isEmpty() && !fetchNextPage()) {
            return null
        }
        return buffer.removeFirst()
    }

    private fun fetchNextPage(): Boolean {
        if (exhausted) return false
        val medias = mediaQueryRepository.findDeletedBefore(purgeCutoff, cursorId, pageSize)
        if (medias.isEmpty()) {
            exhausted = true
            return false
        }
        cursorId = medias.last().id
        buffer.addAll(medias)
        return true
    }
}
