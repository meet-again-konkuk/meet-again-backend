package com.konkuk.ma.job.domain.community

import com.konkuk.ma.domain.community.domain.image.PostImage
import com.konkuk.ma.domain.community.domain.image.PostImagePurger
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter

class PostImagePurgeItemWriter(
    private val postImagePurger: PostImagePurger,
) : ItemWriter<PostImage> {

    override fun write(chunk: Chunk<out PostImage>) {
        chunk.items.forEach(postImagePurger::purge)
    }
}
