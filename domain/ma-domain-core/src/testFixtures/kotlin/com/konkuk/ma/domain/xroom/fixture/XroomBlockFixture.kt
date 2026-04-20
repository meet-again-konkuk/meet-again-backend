package com.konkuk.ma.domain.xroom.fixture

import com.konkuk.ma.domain.xroom.domain.block.NewDdayBlock
import com.konkuk.ma.domain.xroom.domain.block.NewLongTextBlock
import com.konkuk.ma.domain.xroom.domain.block.NewMusicBlock
import com.konkuk.ma.domain.xroom.domain.block.NewPhotoBlock
import com.konkuk.ma.domain.xroom.domain.block.NewShortTextBlock
import com.konkuk.ma.domain.xroom.domain.block.NewVideoBlock
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockItem
import java.time.LocalDate

object XroomBlockFixture {

    fun newPhotoBlock(
        xroomId: Long = 1L,
        item: XroomBlockItem = XroomBlockItem.POLAROID_FRAME,
        positionX: Int = 100,
        positionY: Int = 100,
        rotation: Int = 0,
        photoDate: LocalDate? = LocalDate.of(2024, 3, 21),
    ): NewPhotoBlock {
        return NewPhotoBlock(
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            photoDate = photoDate,
        )
    }

    fun newShortTextBlock(
        xroomId: Long = 1L,
        item: XroomBlockItem = XroomBlockItem.PLAIN_CARD,
        positionX: Int = 100,
        positionY: Int = 100,
        rotation: Int = 0,
        content: String = "짧은 카드 내용",
    ): NewShortTextBlock {
        return NewShortTextBlock(
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            content = content,
        )
    }

    fun newLongTextBlock(
        xroomId: Long = 1L,
        item: XroomBlockItem = XroomBlockItem.LETTER_PAPER,
        positionX: Int = 100,
        positionY: Int = 100,
        rotation: Int = 0,
        content: String = "긴 편지 내용",
    ): NewLongTextBlock {
        return NewLongTextBlock(
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            content = content,
        )
    }

    fun newMusicBlock(
        xroomId: Long = 1L,
        item: XroomBlockItem = XroomBlockItem.MUSIC_CARD,
        positionX: Int = 100,
        positionY: Int = 100,
        rotation: Int = 0,
        musicUrl: String = "https://music.example.com/song.mp3",
        title: String = "노래 제목",
        artist: String? = "가수",
    ): NewMusicBlock {
        return NewMusicBlock(
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

    fun newDdayBlock(
        xroomId: Long = 1L,
        item: XroomBlockItem = XroomBlockItem.DDAY_BADGE,
        positionX: Int = 100,
        positionY: Int = 100,
        rotation: Int = 0,
        anniversaryDate: LocalDate = LocalDate.of(2024, 1, 1),
        label: String = "사귄지",
    ): NewDdayBlock {
        return NewDdayBlock(
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            anniversaryDate = anniversaryDate,
            label = label,
        )
    }

    fun newVideoBlock(
        xroomId: Long = 1L,
        item: XroomBlockItem = XroomBlockItem.VIDEO_FRAME,
        positionX: Int = 100,
        positionY: Int = 100,
        rotation: Int = 0,
        description: String? = "영상 설명",
    ): NewVideoBlock {
        return NewVideoBlock(
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            description = description,
        )
    }
}
