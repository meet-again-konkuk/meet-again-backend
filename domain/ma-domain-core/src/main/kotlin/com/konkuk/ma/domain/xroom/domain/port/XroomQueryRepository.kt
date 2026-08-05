package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.xroom.domain.Xroom

interface XroomQueryRepository {
    fun exists(targetInfoId: Long): Boolean

    fun exists(targetInfoIds: Set<Long>): Set<Long>

    fun find(ownerId: Long): List<Xroom>

    fun findByTargetInfoIds(targetInfoIds: Set<Long>): List<Xroom>

    fun findByTargetInfoIdOrNull(targetInfoId: Long): Xroom?

    fun findOne(xroomId: Long): Xroom
}
