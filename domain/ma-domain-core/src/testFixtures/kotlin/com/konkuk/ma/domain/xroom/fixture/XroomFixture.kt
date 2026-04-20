package com.konkuk.ma.domain.xroom.fixture

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.xroom.domain.Xroom
import com.konkuk.ma.domain.xroom.domain.XroomTheme
import java.time.LocalDateTime

object XroomFixture {

    fun create(
        id: Long = 1L,
        ownerEmail: String = "owner@example.com",
        targetInfoId: Long = 1L,
        theme: XroomTheme = XroomTheme.CORK_BOARD,
        createdDate: LocalDateTime = LocalDateTime.now(),
    ): Xroom {
        return Xroom(
            id = id,
            ownerEmail = Email(ownerEmail),
            targetInfoId = targetInfoId,
            theme = theme,
            createdDate = createdDate,
        )
    }
}
