package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.matching.domain.NewTargetInfo

interface TargetInfoCommandRepository {
    fun save(newTargetInfo: NewTargetInfo): Long
}
