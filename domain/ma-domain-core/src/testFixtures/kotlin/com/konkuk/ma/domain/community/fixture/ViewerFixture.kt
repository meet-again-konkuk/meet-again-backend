package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.community.domain.LikedIds
import com.konkuk.ma.domain.community.domain.Viewer

object ViewerFixture {
    fun create(
        viewerId: Long = 1L,
        likedIds: Set<Long> = emptySet(),
    ): Viewer {
        return Viewer(
            viewerId = viewerId,
            likedIds = LikedIds(likedIds),
        )
    }
}
