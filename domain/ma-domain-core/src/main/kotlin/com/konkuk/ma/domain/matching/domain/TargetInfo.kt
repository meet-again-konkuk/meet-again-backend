package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.member.domain.Region
import java.time.LocalDate

class TargetInfo(
    val targetInfoId: Long,
    val memberId: Long,
    val name: String,
    val phoneNumber: String?,
    val birthDate: LocalDate?,
    val region: Region?
) {
}
