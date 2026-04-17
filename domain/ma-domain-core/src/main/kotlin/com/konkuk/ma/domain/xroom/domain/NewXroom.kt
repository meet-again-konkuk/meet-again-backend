package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.common.domain.Email

class NewXroom(
    val ownerEmail: Email,
    val targetInfoId: Long,
    val theme: XroomTheme = XroomTheme.CORK_BOARD,
)
