package com.konkuk.ma.domain.xroom.domain

class NewXroom(
    val ownerId: Long,
    val targetInfoId: Long,
    val theme: XroomTheme = XroomTheme.CORK_BOARD,
)
