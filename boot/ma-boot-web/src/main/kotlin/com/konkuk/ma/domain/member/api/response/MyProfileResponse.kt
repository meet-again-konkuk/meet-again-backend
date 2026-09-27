package com.konkuk.ma.domain.member.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.MemberProfile
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.support.id.EncryptId
import java.time.LocalDate

class MyProfileResponse(
    @EncryptId(ObfuscationType.MEMBER)
    val memberId: Long,
    val email: String,
    val nickname: String,
    val name: String,
    val gender: Gender,
    val birthDate: LocalDate,
    val phoneNumber: String,
    val region: Region,
    val highSchool: String?,
    val university: String?,
    val profileImageUrl: String?,
) {
    companion object {
        fun from(profile: MemberProfile): MyProfileResponse {
            val member = profile.member
            return MyProfileResponse(
                memberId = member.id,
                email = member.email.value,
                nickname = member.nickname,
                name = member.name,
                gender = member.gender,
                birthDate = member.birthDate,
                phoneNumber = member.phoneNumber.fullNumber,
                region = member.region,
                highSchool = member.highSchool,
                university = member.university,
                profileImageUrl = profile.profileImageUrl,
            )
        }
    }
}
