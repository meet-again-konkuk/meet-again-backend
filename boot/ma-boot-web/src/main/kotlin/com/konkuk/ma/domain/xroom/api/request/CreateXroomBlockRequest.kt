package com.konkuk.ma.domain.xroom.api.request

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.konkuk.ma.domain.xroom.domain.block.NewDdayBlock
import com.konkuk.ma.domain.xroom.domain.block.NewLongTextBlock
import com.konkuk.ma.domain.xroom.domain.block.NewMusicBlock
import com.konkuk.ma.domain.xroom.domain.block.NewPhotoBlock
import com.konkuk.ma.domain.xroom.domain.block.NewShortTextBlock
import com.konkuk.ma.domain.xroom.domain.block.NewVideoBlock
import com.konkuk.ma.domain.xroom.domain.block.NewXroomBlock
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockItem
import java.time.LocalDate

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = CreatePhotoBlockRequest::class, name = "PHOTO"),
    JsonSubTypes.Type(value = CreateShortTextBlockRequest::class, name = "SHORT_TEXT"),
    JsonSubTypes.Type(value = CreateLongTextBlockRequest::class, name = "LONG_TEXT"),
    JsonSubTypes.Type(value = CreateMusicBlockRequest::class, name = "MUSIC"),
    JsonSubTypes.Type(value = CreateDdayBlockRequest::class, name = "DDAY"),
    JsonSubTypes.Type(value = CreateVideoBlockRequest::class, name = "VIDEO"),
)
sealed class CreateXroomBlockRequest {
    abstract val item: XroomBlockItem
    abstract val positionX: Int
    abstract val positionY: Int
    abstract val rotation: Int

    abstract fun toCommand(xroomId: Long): NewXroomBlock
}

@JsonTypeName("PHOTO")
data class CreatePhotoBlockRequest(
    override val item: XroomBlockItem,
    override val positionX: Int,
    override val positionY: Int,
    override val rotation: Int,
    val photoDate: LocalDate? = null,
) : CreateXroomBlockRequest() {
    override fun toCommand(xroomId: Long): NewXroomBlock {
        return NewPhotoBlock(
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            photoDate = photoDate,
        )
    }
}

@JsonTypeName("SHORT_TEXT")
data class CreateShortTextBlockRequest(
    override val item: XroomBlockItem,
    override val positionX: Int,
    override val positionY: Int,
    override val rotation: Int,
    val content: String,
) : CreateXroomBlockRequest() {
    override fun toCommand(xroomId: Long): NewXroomBlock {
        return NewShortTextBlock(
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            content = content,
        )
    }
}

@JsonTypeName("LONG_TEXT")
data class CreateLongTextBlockRequest(
    override val item: XroomBlockItem,
    override val positionX: Int,
    override val positionY: Int,
    override val rotation: Int,
    val content: String,
) : CreateXroomBlockRequest() {
    override fun toCommand(xroomId: Long): NewXroomBlock {
        return NewLongTextBlock(
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            content = content,
        )
    }
}

@JsonTypeName("MUSIC")
data class CreateMusicBlockRequest(
    override val item: XroomBlockItem,
    override val positionX: Int,
    override val positionY: Int,
    override val rotation: Int,
    val musicUrl: String,
    val title: String,
    val artist: String? = null,
) : CreateXroomBlockRequest() {
    override fun toCommand(xroomId: Long): NewXroomBlock {
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
}

@JsonTypeName("DDAY")
data class CreateDdayBlockRequest(
    override val item: XroomBlockItem,
    override val positionX: Int,
    override val positionY: Int,
    override val rotation: Int,
    val anniversaryDate: LocalDate,
    val label: String,
) : CreateXroomBlockRequest() {
    override fun toCommand(xroomId: Long): NewXroomBlock {
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
}

@JsonTypeName("VIDEO")
data class CreateVideoBlockRequest(
    override val item: XroomBlockItem,
    override val positionX: Int,
    override val positionY: Int,
    override val rotation: Int,
    val description: String? = null,
) : CreateXroomBlockRequest() {
    override fun toCommand(xroomId: Long): NewXroomBlock {
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
