package com.konkuk.ma.domain.withdrawal.domain

import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.Region
import java.time.LocalDate
import java.time.LocalDateTime

class MemberBackupView(
    val id: Long,
    val email: String,
    val nickname: String,
    val name: String,
    val gender: Gender,
    val maskedPhoneNumber: String,
    val region: Region,
    val birthDate: LocalDate,
    val highSchool: String?,
    val university: String?,
    val withdrawalRequestedAt: LocalDateTime?,
) {
    companion object {
        fun from(member: Member): MemberBackupView {
            return MemberBackupView(
                id = member.id,
                email = member.email.value,
                nickname = member.nickname,
                name = member.name,
                gender = member.gender,
                maskedPhoneNumber = member.maskedPhoneNumber(),
                region = member.region,
                birthDate = member.birthDate,
                highSchool = member.highSchool,
                university = member.university,
                withdrawalRequestedAt = member.withdrawalRequestedAt,
            )
        }
    }
}
