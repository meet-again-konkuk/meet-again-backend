package com.konkuk.ma.domain.matching.api.request

import com.konkuk.ma.domain.common.domain.Day
import com.konkuk.ma.domain.common.domain.Month
import com.konkuk.ma.domain.common.domain.Year
import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.member.domain.Region
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

class NewTargetInfoRequest(
    @field:NotBlank(message = "이름은 필수입니다.")
    @field:Pattern(regexp = "^[가-힣]{2,10}$", message = "이름은 한글 2자 이상 10자 이하여야 합니다.")
    val name: String,

    val middleNumber: String?,

    val lastNumber: String?,

    val year: Int?,
    val month: Int?,
    val day: Int?,

    val region: Region?
) {
    fun toNewTargetInfo(registerEmail: String): NewTargetInfo {
        return NewTargetInfo(
            registerEmail = registerEmail,
            targetName = name,
            middleNumber = middleNumber,
            lastNumber = lastNumber,
            year = year?.let { Year(it) },
            month = month?.let { Month(it) },
            day = day?.let { Day(it) },
            region = region
        )
    }
}
