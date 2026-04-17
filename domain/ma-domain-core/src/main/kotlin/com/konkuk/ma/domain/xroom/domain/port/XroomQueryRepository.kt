package com.konkuk.ma.domain.xroom.domain.port

interface XroomQueryRepository {
    fun exists(targetInfoId: Long): Boolean
}
