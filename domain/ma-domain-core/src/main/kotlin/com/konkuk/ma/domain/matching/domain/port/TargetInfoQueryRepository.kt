package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.domain.TargetInfo

interface TargetInfoQueryRepository {
    fun findOne(id: Long): TargetInfo
    fun find(email: Email): List<TargetInfo>
    fun findNoOffset(cursorId: Long?, size: Int): List<TargetInfo>
}
