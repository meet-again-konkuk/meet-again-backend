package com.konkuk.ma.domain.xroom.fixture

import com.konkuk.ma.domain.xroom.domain.MyXroom
import com.konkuk.ma.domain.xroom.domain.Xroom

object MyXroomFixture {
    fun create(
        xroom: Xroom = XroomFixture.create(),
        recipientName: String = "김만남",
        memoryCount: Int = 0,
    ): MyXroom {
        return MyXroom(
            xroom = xroom,
            recipientName = recipientName,
            memoryCount = memoryCount,
        )
    }
}
