package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.xroom.domain.Xroom

interface XroomQueryRepository {
    fun exists(targetInfoId: Long): Boolean

    fun exists(targetInfoIds: Set<Long>): Set<Long>

    fun findOne(xroomId: Long): Xroom
}
