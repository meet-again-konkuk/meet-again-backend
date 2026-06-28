package com.konkuk.ma.domain.xroom.domain

class XroomDetail(
    private val xroom: Xroom,
    val recipientName: String,
) {
    val id: Long get() = xroom.id
    val title: String get() = xroom.titleValue
    val template: String get() = xroom.templateValue
    val finalMessage: String? get() = xroom.finalMessageValue
}
