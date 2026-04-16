package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.common.domain.Email

class NewXroom(
    ownerEmail: String,
    val targetInfoId: Long,
    val theme: XroomTheme = XroomTheme.CORK_BOARD,
) {
    val ownerEmail: Email = Email(ownerEmail)
}
