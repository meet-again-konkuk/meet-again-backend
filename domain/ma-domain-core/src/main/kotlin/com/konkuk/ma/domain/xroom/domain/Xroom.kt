package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityType
import java.time.LocalDateTime

class Xroom(
    val id: Long,
    val ownerEmail: Email,
    val targetInfoId: Long,
    val theme: XroomTheme,
    val createdDate: LocalDateTime,
) {
    fun validateOwnership(email: Email) {
        if (ownerEmail != email) {
            throw AccessDeniedException(EntityType.XROOM, ownerEmail.value, email.value)
        }
    }
}
