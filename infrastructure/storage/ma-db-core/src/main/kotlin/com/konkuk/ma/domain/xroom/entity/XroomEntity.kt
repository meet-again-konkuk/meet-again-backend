package com.konkuk.ma.domain.xroom.entity

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.xroom.domain.Xroom
import com.konkuk.ma.domain.xroom.domain.XroomTheme
import com.konkuk.ma.domain.xroom.entity.table.XroomTable
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.ResultRow

class XroomEntity(
    val id: Long,
    val ownerEmail: String,
    val targetInfoId: Long,
    val theme: XroomTheme,
    val createdDate: LocalDateTime,
) {
    fun toDomain(): Xroom {
        return Xroom(
            id = id,
            ownerEmail = Email(ownerEmail),
            targetInfoId = targetInfoId,
            theme = theme,
            createdDate = createdDate,
        )
    }

    companion object {
        fun from(row: ResultRow): XroomEntity {
            return XroomEntity(
                id = row[XroomTable.id].value,
                ownerEmail = row[XroomTable.ownerEmail],
                targetInfoId = row[XroomTable.targetInfoId],
                theme = XroomTheme.valueOf(row[XroomTable.theme]),
                createdDate = row[XroomTable.createdDate],
            )
        }
    }
}
