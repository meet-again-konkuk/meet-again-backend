package com.konkuk.ma.domain.xroom.entity

import com.konkuk.ma.domain.xroom.domain.FinalMessage
import com.konkuk.ma.domain.xroom.domain.Xroom
import com.konkuk.ma.domain.xroom.domain.XroomTemplate
import com.konkuk.ma.domain.xroom.domain.XroomTitle
import com.konkuk.ma.domain.xroom.entity.table.XroomTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime

class XroomEntity(
    val id: Long,
    val ownerId: Long,
    val targetInfoId: Long,
    val template: String,
    val title: String,
    val finalMessage: String?,
    val createdDate: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    fun toDomain(): Xroom {
        return Xroom(
            id = id,
            ownerId = ownerId,
            targetInfoId = targetInfoId,
            title = XroomTitle(title),
            template = XroomTemplate.from(template),
            finalMessage = FinalMessage.of(finalMessage),
            createdDate = createdDate,
            updatedAt = updatedAt,
        )
    }

    companion object {
        fun from(row: ResultRow): XroomEntity {
            return XroomEntity(
                id = row[XroomTable.id].value,
                ownerId = row[XroomTable.ownerId],
                targetInfoId = row[XroomTable.targetInfoId],
                template = row[XroomTable.template],
                title = row[XroomTable.title],
                finalMessage = row[XroomTable.finalMessage],
                createdDate = row[XroomTable.createdDate],
                updatedAt = row[XroomTable.lastModifiedDate],
            )
        }
    }
}
