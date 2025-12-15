package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.matching.domain.port.TargetInfoCommandRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TargetInfoCommandService(
    private val targetInfoCommandRepository: TargetInfoCommandRepository
) {
    fun register(targetInfo: NewTargetInfo): Long {
        return targetInfoCommandRepository.save(targetInfo)
    }
}
