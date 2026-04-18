package com.konkuk.ma.domain.xroom.domain.port

interface XroomQueryRepository {
    fun exists(targetInfoId: Long): Boolean

    fun exists(targetInfoIds: Set<Long>): Set<Long>
}
