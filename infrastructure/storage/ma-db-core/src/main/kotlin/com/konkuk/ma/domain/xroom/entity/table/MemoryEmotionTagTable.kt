package com.konkuk.ma.domain.xroom.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object MemoryEmotionTagTable : BaseTable("MEMORY_EMOTION_TAGS", "TAG_ID") {
    val memoryId = long("MEMORY_ID")
    val tag = varchar("TAG", 50)
}
