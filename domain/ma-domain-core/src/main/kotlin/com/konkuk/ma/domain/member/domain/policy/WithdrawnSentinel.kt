package com.konkuk.ma.domain.member.domain.policy

import com.konkuk.ma.domain.member.domain.Region
import java.time.LocalDate

object WithdrawnSentinel {
    const val PASSWORD = ""
    const val NAME = "탈퇴한회원"
    const val PHONE_NUMBER = "01000000000"
    val BIRTH_DATE: LocalDate = LocalDate.of(1900, 1, 1)
    val REGION: Region = Region.SEOUL

    private const val NICKNAME_PREFIX = "탈퇴한회원_"

    fun nickname(memberId: Long): String = "$NICKNAME_PREFIX$memberId"
}
