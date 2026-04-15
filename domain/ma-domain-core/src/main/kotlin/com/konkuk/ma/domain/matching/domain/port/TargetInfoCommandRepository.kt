package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.member.domain.Gender

interface TargetInfoCommandRepository {
    fun save(newTargetInfo: NewTargetInfo, targetGender: Gender): Long
    fun update(targetInfo: TargetInfo)
}
