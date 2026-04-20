package com.konkuk.ma.domain.xroom.fixture

import com.konkuk.ma.domain.xroom.domain.block.DdayBlock
import com.konkuk.ma.domain.xroom.domain.block.LongTextBlock
import com.konkuk.ma.domain.xroom.domain.block.MusicBlock
import com.konkuk.ma.domain.xroom.domain.block.Photo
import com.konkuk.ma.domain.xroom.domain.block.PhotoBlock
import com.konkuk.ma.domain.xroom.domain.block.ShortTextBlock
import com.konkuk.ma.domain.xroom.domain.block.Video
import com.konkuk.ma.domain.xroom.domain.block.VideoBlock
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockItem
import java.time.LocalDate

object XroomBlockDomainFixture {

    fun photoBlock(
        id: Long = 100L,
        xroomId: Long = 1L,
        item: XroomBlockItem = XroomBlockItem.POLAROID_FRAME,
        positionX: Int = 100,
        positionY: Int = 100,
        rotation: Int = 0,
        photoDate: LocalDate? = LocalDate.of(2024, 3, 21),
    ): PhotoBlock {
        return PhotoBlock(
            id = id,
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            photoDate = photoDate,
        )
    }

    fun shortTextBlock(
        id: Long = 100L,
        xroomId: Long = 1L,
        item: XroomBlockItem = XroomBlockItem.PLAIN_CARD,
        positionX: Int = 100,
        positionY: Int = 100,
        rotation: Int = 0,
        content: String = "짧은 카드 내용",
    ): ShortTextBlock {
        return ShortTextBlock(
            id = id,
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            content = content,
        )
    }

    fun longTextBlock(
        id: Long = 100L,
        xroomId: Long = 1L,
        item: XroomBlockItem = XroomBlockItem.LETTER_PAPER,
        positionX: Int = 100,
        positionY: Int = 100,
        rotation: Int = 0,
        content: String = "긴 편지 내용",
    ): LongTextBlock {
        return LongTextBlock(
            id = id,
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            content = content,
        )
    }

    fun musicBlock(
        id: Long = 100L,
        xroomId: Long = 1L,
        item: XroomBlockItem = XroomBlockItem.MUSIC_CARD,
        positionX: Int = 100,
        positionY: Int = 100,
        rotation: Int = 0,
        musicUrl: String = "https://music.example.com/song.mp3",
        title: String = "노래 제목",
        artist: String? = "가수",
    ): MusicBlock {
        return MusicBlock(
            id = id,
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            musicUrl = musicUrl,
            title = title,
            artist = artist,
        )
    }

    fun ddayBlock(
        id: Long = 100L,
        xroomId: Long = 1L,
        item: XroomBlockItem = XroomBlockItem.DDAY_BADGE,
        positionX: Int = 100,
        positionY: Int = 100,
        rotation: Int = 0,
        anniversaryDate: LocalDate = LocalDate.of(2024, 1, 1),
        label: String = "사귄지",
    ): DdayBlock {
        return DdayBlock(
            id = id,
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            anniversaryDate = anniversaryDate,
            label = label,
        )
    }

    fun videoBlock(
        id: Long = 100L,
        xroomId: Long = 1L,
        item: XroomBlockItem = XroomBlockItem.VIDEO_FRAME,
        positionX: Int = 100,
        positionY: Int = 100,
        rotation: Int = 0,
        description: String? = "영상 설명",
    ): VideoBlock {
        return VideoBlock(
            id = id,
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            description = description,
        )
    }

    fun photo(
        id: Long = 1000L,
        blockId: Long = 100L,
        photoUrl: String = "https://cdn.example.com/photo.jpg",
        orderIndex: Int = 0,
    ): Photo {
        return Photo(
            id = id,
            blockId = blockId,
            photoUrl = photoUrl,
            orderIndex = orderIndex,
        )
    }

    fun video(
        id: Long = 1000L,
        blockId: Long = 100L,
        videoUrl: String = "https://cdn.example.com/video.mp4",
        orderIndex: Int = 0,
    ): Video {
        return Video(
            id = id,
            blockId = blockId,
            videoUrl = videoUrl,
            orderIndex = orderIndex,
        )
    }
}
