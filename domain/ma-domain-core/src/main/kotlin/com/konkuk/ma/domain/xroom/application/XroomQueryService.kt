package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.matching.domain.TargetInfos
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.xroom.domain.MyXrooms
import com.konkuk.ma.domain.xroom.domain.Xrooms
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class XroomQueryService(
    private val xroomQueryRepository: XroomQueryRepository,
    private val targetInfoQueryRepository: TargetInfoQueryRepository,
) {
    fun findMine(memberId: Long): MyXrooms {
        val xrooms = Xrooms(xroomQueryRepository.find(memberId))
        val targetInfos = TargetInfos(targetInfoQueryRepository.find(memberId))
        return xrooms.toMine(targetInfos, NO_MEMORY_COUNTS)
    }

    companion object {
        private val NO_MEMORY_COUNTS = emptyMap<Long, Int>()
    }
}
