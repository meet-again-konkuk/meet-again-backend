package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.matching.domain.TargetInfo

interface TargetInfoQueryRepository {
    fun find(email: String): List<TargetInfo>
    fun findNoOffset(cursorId: Long?, size: Int): List<TargetInfo>
}
