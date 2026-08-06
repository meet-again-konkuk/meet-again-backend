package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.xroom.domain.NewXroom
import com.konkuk.ma.domain.xroom.domain.Xroom

interface XroomCommandRepository {
    fun save(newXroom: NewXroom): Long

    fun updateFinalMessage(xroom: Xroom)

    fun delete(ownerId: Long)

    fun deleteById(xroomId: Long, memberId: Long)
}
