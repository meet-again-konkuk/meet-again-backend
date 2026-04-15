package com.konkuk.ma.domain.matching.domain.port

import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.matching.domain.UpdateTargetInfo
import com.konkuk.ma.domain.member.domain.Gender

interface TargetInfoCommandRepository {
    fun save(newTargetInfo: NewTargetInfo, targetGender: Gender): Long
    fun update(id: Long, email: String, updateTargetInfo: UpdateTargetInfo)
    fun delete(id: Long)
}
