package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.xroom.domain.memory.Memories
import com.konkuk.ma.domain.xroom.domain.memory.Memory

class XroomDetail(
    private val xroom: Xroom,
    val recipientName: String,
    private val memoriesCollection: Memories,
) {
    val id: Long get() = xroom.id
    val title: String get() = xroom.titleValue
    val template: String get() = xroom.templateValue
    val finalMessage: String? get() = xroom.finalMessageValue
    val memories: List<Memory> get() = memoriesCollection.sortedByEventDate()
}
