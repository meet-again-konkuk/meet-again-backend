package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.xroom.domain.block.NewXroomBlock

interface XroomBlockCommandRepository {
    fun save(newBlock: NewXroomBlock): Long
}
