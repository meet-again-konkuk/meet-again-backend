package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.matching.domain.TargetInfo

interface TargetInfoQueryRepository {
    fun findOne(id: Long): TargetInfo
    fun find(memberId: Long): List<TargetInfo>
    fun findNoOffset(cursorId: Long?, size: Int): List<TargetInfo>
}
