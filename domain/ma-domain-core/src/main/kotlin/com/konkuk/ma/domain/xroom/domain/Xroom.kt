package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityType
import java.time.LocalDateTime

class Xroom(
    val id: Long,
    val ownerId: Long,
    val targetInfoId: Long,
    val theme: XroomTheme,
    val createdDate: LocalDateTime,
) {
    fun validateOwnership(memberId: Long) {
        if (ownerId != memberId) {
            throw AccessDeniedException(EntityType.XROOM, ownerId.toString(), memberId.toString())
        }
    }
}
