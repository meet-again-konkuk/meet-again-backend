package com.konkuk.ma.domain.xroom.domain

class ReceivedXroom(
    private val xroom: Xroom,
    val senderName: String,
    val memoryCount: Int,
) {
    val id: Long get() = xroom.id
    val title: String get() = xroom.titleValue
}
