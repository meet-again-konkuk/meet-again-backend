package com.konkuk.ma.domain.member.api.request

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.konkuk.ma.domain.common.domain.Changed
import com.konkuk.ma.domain.member.domain.ProfileChanges
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.support.validation.ValidationMessages
import com.konkuk.ma.support.validation.ValidationPatterns
import jakarta.validation.constraints.Pattern
import java.util.Optional

class UpdateMyProfileRequest(
    @field:Pattern(regexp = ValidationPatterns.NICKNAME, message = ValidationMessages.NICKNAME_INVALID)
    @field:JsonSetter(nulls = Nulls.FAIL)
    val nickname: String? = null,
    @field:JsonSetter(nulls = Nulls.FAIL)
    val region: Region? = null,
    val highSchool: Optional<String>? = null,
    val university: Optional<String>? = null,
) {
    fun toProfileChanges(): ProfileChanges {
        return ProfileChanges(
            nickname = nickname,
            region = region,
            highSchool = highSchool?.let { Changed(it.orElse(null)) },
            university = university?.let { Changed(it.orElse(null)) },
        )
    }
}
