package com.konkuk.ma.domain.xroom.domain

import java.time.LocalDateTime

class MyXroom(
    private val xroom: Xroom,
    val recipientName: String,
    val memoryCount: Int,
) {
    val id: Long get() = xroom.id
    val title: String get() = xroom.title.value
    val targetInfoId: Long get() = xroom.targetInfoId
    val updatedAt: LocalDateTime get() = xroom.updatedAt
}
