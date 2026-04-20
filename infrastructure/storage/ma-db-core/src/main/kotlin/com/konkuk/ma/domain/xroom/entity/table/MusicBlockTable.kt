package com.konkuk.ma.domain.xroom.entity.table

import org.jetbrains.exposed.dao.id.LongIdTable

object MusicBlockTable : LongIdTable("XROOM_MUSIC_BLOCKS", "XROOM_BLOCK_ID") {
    val musicUrl = varchar("MUSIC_URL", 512)
    val title = varchar("TITLE", 255)
    val artist = varchar("ARTIST", 255).nullable()
}
