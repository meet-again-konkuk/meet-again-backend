package com.konkuk.ma.domain.xroom.domain.port

interface XroomQueryRepository {
    fun existsByTargetInfoId(targetInfoId: Long): Boolean
}
