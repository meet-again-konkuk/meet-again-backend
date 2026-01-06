package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.matching.domain.TargetInfo

interface TargetInfoQueryRepository {
    fun findNoOffset(cursorId: Long?, size: Int): List<TargetInfo>
}
