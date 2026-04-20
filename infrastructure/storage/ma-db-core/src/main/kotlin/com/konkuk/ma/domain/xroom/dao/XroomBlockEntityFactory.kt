package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.domain.block.XroomBlockType
import com.konkuk.ma.domain.xroom.entity.XroomBlockEntity
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow

interface XroomBlockEntityFactory {
    val type: XroomBlockType
    val childTable: LongIdTable

    fun createFrom(row: ResultRow): XroomBlockEntity
}
