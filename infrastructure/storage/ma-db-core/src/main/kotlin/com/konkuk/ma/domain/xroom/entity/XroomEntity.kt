package com.konkuk.ma.domain.xroom.entity

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.xroom.domain.Xroom
import com.konkuk.ma.domain.xroom.domain.XroomTheme
import java.time.LocalDateTime

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
}
