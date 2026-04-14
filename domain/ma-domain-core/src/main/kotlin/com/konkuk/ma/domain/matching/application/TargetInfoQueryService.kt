package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TargetInfoQueryService(
    private val targetInfoQueryRepository: TargetInfoQueryRepository,
) {
    fun find(email: String): List<TargetInfo> {
        return targetInfoQueryRepository.find(Email(email))
    }

    fun findDetail(id: Long, email: String): TargetInfo {
        val targetInfo = targetInfoQueryRepository.findOne(id)
        targetInfo.validateOwnership(Email(email))
        return targetInfo
    }
}
