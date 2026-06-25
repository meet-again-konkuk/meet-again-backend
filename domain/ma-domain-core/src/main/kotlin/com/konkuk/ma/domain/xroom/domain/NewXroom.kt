package com.konkuk.ma.domain.xroom.domain

class NewXroom(
    val ownerId: Long,
    val targetInfoId: Long,
    finalMessage: String? = null,
) {
    val title: XroomTitle = XroomTitle.DEFAULT
    val template: XroomTemplate = XroomTemplate.DEFAULT
    val finalMessage: FinalMessage? = FinalMessage.of(finalMessage)
}
