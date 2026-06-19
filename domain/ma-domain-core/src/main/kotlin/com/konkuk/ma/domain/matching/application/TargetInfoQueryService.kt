package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TargetInfoQueryService(
    private val targetInfoQueryRepository: TargetInfoQueryRepository,
) {
    fun find(memberId: Long): List<TargetInfo> {
        return targetInfoQueryRepository.find(memberId)
    }

    fun findDetail(id: Long, memberId: Long): TargetInfo {
        val targetInfo = targetInfoQueryRepository.findOne(id)
        targetInfo.validateOwnership(memberId)
        return targetInfo
    }
}
