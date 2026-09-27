package com.konkuk.ma.job.domain.community

import com.konkuk.ma.domain.community.domain.image.PostImage
import com.konkuk.ma.domain.community.domain.port.PostImageQueryRepository
import java.time.LocalDateTime
import org.springframework.batch.item.ItemReader

open class DeletedPostImageItemReader(
    private val postImageQueryRepository: PostImageQueryRepository,
    private val purgeCutoff: LocalDateTime,
    private val pageSize: Int,
) : ItemReader<PostImage> {

    private val buffer: ArrayDeque<PostImage> = ArrayDeque()
    private var cursorId: Long? = null
    private var exhausted: Boolean = false

    override fun read(): PostImage? {
        if (buffer.isEmpty() && !fetchNextPage()) {
            return null
        }
        return buffer.removeFirst()
    }

    private fun fetchNextPage(): Boolean {
        if (exhausted) return false
        val images = postImageQueryRepository.findDeletedBefore(purgeCutoff, cursorId, pageSize)
        if (images.isEmpty()) {
            exhausted = true
            return false
        }
        cursorId = images.last().id
        buffer.addAll(images)
        return true
    }
}
