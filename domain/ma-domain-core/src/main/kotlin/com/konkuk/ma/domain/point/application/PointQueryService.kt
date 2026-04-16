package com.konkuk.ma.domain.point.application

import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.port.PointProductQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PointQueryService(
    private val pointProductQueryRepository: PointProductQueryRepository,
) {
    fun findProducts(): List<PointProduct> {
        return pointProductQueryRepository.find()
    }
}
