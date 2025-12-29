package com.konkuk.ma.domain.matching.repository

import com.konkuk.ma.domain.matching.dao.TargetInfoCommandDao
import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.matching.domain.port.TargetInfoCommandRepository
import org.springframework.stereotype.Repository

@Repository
class TargetInfoCommandCoreRepository(
    private val targetInfoCommandDao: TargetInfoCommandDao
) : TargetInfoCommandRepository {
    override fun save(newTargetInfo: NewTargetInfo): Long {
        return targetInfoCommandDao.save(newTargetInfo)
    }
}
