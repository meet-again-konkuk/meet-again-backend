package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.domain.block.NewDdayBlock
import com.konkuk.ma.domain.xroom.domain.block.NewLongTextBlock
import com.konkuk.ma.domain.xroom.domain.block.NewMusicBlock
import com.konkuk.ma.domain.xroom.domain.block.NewPhotoBlock
import com.konkuk.ma.domain.xroom.domain.block.NewShortTextBlock
import com.konkuk.ma.domain.xroom.domain.block.NewVideoBlock
import com.konkuk.ma.domain.xroom.domain.block.NewXroomBlock
import com.konkuk.ma.domain.xroom.entity.table.DdayBlockTable
import com.konkuk.ma.domain.xroom.entity.table.LongTextBlockTable
import com.konkuk.ma.domain.xroom.entity.table.MusicBlockTable
import com.konkuk.ma.domain.xroom.entity.table.PhotoBlockTable
import com.konkuk.ma.domain.xroom.entity.table.ShortTextBlockTable
import com.konkuk.ma.domain.xroom.entity.table.VideoBlockTable
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class XroomBlockCommandDao {
    fun save(newBlock: NewXroomBlock): Long {
        val parentId = insertParent(newBlock)
        insertChild(parentId, newBlock)
        return parentId
    }

    private fun insertParent(newBlock: NewXroomBlock): Long {
        return XroomBlockTable.insertAndGetId {
            it[xroomId] = newBlock.xroomId
            it[blockType] = newBlock.type.name
            it[item] = newBlock.item.name
            it[positionX] = newBlock.positionX.toUByte()
            it[positionY] = newBlock.positionY.toUByte()
            it[rotation] = newBlock.rotation
        }.value
    }

    private fun insertChild(parentId: Long, newBlock: NewXroomBlock) {
        when (newBlock) {
            is NewPhotoBlock -> insertPhotoBlock(parentId, newBlock)
            is NewShortTextBlock -> insertShortTextBlock(parentId, newBlock)
            is NewLongTextBlock -> insertLongTextBlock(parentId, newBlock)
            is NewMusicBlock -> insertMusicBlock(parentId, newBlock)
            is NewDdayBlock -> insertDdayBlock(parentId, newBlock)
            is NewVideoBlock -> insertVideoBlock(parentId, newBlock)
        }
    }

    private fun insertPhotoBlock(parentId: Long, block: NewPhotoBlock) {
        PhotoBlockTable.insert {
            it[id] = EntityID(parentId, PhotoBlockTable)
            it[photoDate] = block.photoDate
        }
    }

    private fun insertShortTextBlock(parentId: Long, block: NewShortTextBlock) {
        ShortTextBlockTable.insert {
            it[id] = EntityID(parentId, ShortTextBlockTable)
            it[content] = block.content
        }
    }

    private fun insertLongTextBlock(parentId: Long, block: NewLongTextBlock) {
        LongTextBlockTable.insert {
            it[id] = EntityID(parentId, LongTextBlockTable)
            it[content] = block.content
        }
    }

    private fun insertMusicBlock(parentId: Long, block: NewMusicBlock) {
        MusicBlockTable.insert {
            it[id] = EntityID(parentId, MusicBlockTable)
            it[musicUrl] = block.musicUrl
            it[title] = block.title
            it[artist] = block.artist
        }
    }

    private fun insertDdayBlock(parentId: Long, block: NewDdayBlock) {
        DdayBlockTable.insert {
            it[id] = EntityID(parentId, DdayBlockTable)
            it[anniversaryDate] = block.anniversaryDate
            it[label] = block.label
        }
    }

    private fun insertVideoBlock(parentId: Long, block: NewVideoBlock) {
        VideoBlockTable.insert {
            it[id] = EntityID(parentId, VideoBlockTable)
            it[description] = block.description
        }
    }
}
