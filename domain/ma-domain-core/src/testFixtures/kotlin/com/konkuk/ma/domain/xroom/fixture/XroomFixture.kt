package com.konkuk.ma.domain.xroom.fixture

import com.konkuk.ma.domain.xroom.domain.FinalMessage
import com.konkuk.ma.domain.xroom.domain.Xroom
import com.konkuk.ma.domain.xroom.domain.XroomTemplate
import com.konkuk.ma.domain.xroom.domain.XroomTitle
import java.time.LocalDateTime

object XroomFixture {
    fun create(
        id: Long = 1L,
        ownerId: Long = 1L,
        targetInfoId: Long = 1L,
        title: XroomTitle = XroomTitle.DEFAULT,
        template: XroomTemplate = XroomTemplate.DEFAULT,
        finalMessage: FinalMessage? = null,
        createdDate: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now(),
    ): Xroom {
        return Xroom(
            id = id,
            ownerId = ownerId,
            targetInfoId = targetInfoId,
            title = title,
            template = template,
            finalMessage = finalMessage,
            createdDate = createdDate,
            updatedAt = updatedAt,
        )
    }
}
