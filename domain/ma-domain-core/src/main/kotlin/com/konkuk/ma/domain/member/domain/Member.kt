package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.member.domain.policy.WithdrawnSentinel
import com.konkuk.ma.domain.member.exception.AlreadyWithdrawalRequestedException
import com.konkuk.ma.domain.member.exception.NotWithdrawalRequestedException
import com.konkuk.ma.domain.member.exception.WithdrawalPendingLoginException
import java.time.LocalDate
import java.time.LocalDateTime

class Member(
    val id: Long = 0L,
    val email: Email,
    val password: String,
    nickname: String,
    val gender: Gender,
    val phoneNumber: PhoneNumber,
    val name: String,
    region: Region,
    val birthDate: LocalDate,
    highSchool: String?,
    university: String?,
    withdrawalRequestedAt: LocalDateTime? = null,
) {
    var nickname: String = nickname
        private set

    var region: Region = region
        private set

    var highSchool: String? = highSchool
        private set

    var university: String? = university
        private set

    var withdrawalRequestedAt: LocalDateTime? = withdrawalRequestedAt
        private set

    companion object {
        fun create(
            id: Long = 0L,
            email: String,
            password: String,
            nickname: String,
            gender: Gender,
            phoneNumber: String,
            name: String,
            region: Region,
            birthDate: LocalDate,
            highSchool: String?,
            university: String?
        ): Member {
            return Member(
                id = id,
                email = Email(email),
                password = password,
                nickname = nickname,
                gender = gender,
                phoneNumber = PhoneNumber(phoneNumber),
                name = name,
                region = region,
                birthDate = birthDate,
                highSchool = highSchool,
                university = university
            )
        }
    }

    fun getOtherGender(): Gender {
        return gender.getOtherGender()
    }

    fun getYear(): Year {
        return Year(birthDate.year)
    }

    fun getMonth(): Month {
        return Month(birthDate.monthValue)
    }

    fun getDay(): Day {
        return Day(birthDate.dayOfMonth)
    }

    fun hasNickname(nickname: String): Boolean = this.nickname == nickname

    fun changeProfile(changes: ProfileChanges) {
        changes.nickname?.let { nickname = it }
        changes.region?.let { region = it }
        changes.highSchool?.let { highSchool = it.value }
        changes.university?.let { university = it.value }
    }

    fun requestWithdrawal(now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        if (withdrawalRequestedAt != null) {
            throw AlreadyWithdrawalRequestedException(email)
        }
        withdrawalRequestedAt = now
        return now
    }

    fun cancelWithdrawal() {
        if (withdrawalRequestedAt == null) {
            throw NotWithdrawalRequestedException(email)
        }
        withdrawalRequestedAt = null
    }

    fun verifyLogin() {
        if (isWithdrawalRequested()) {
            throw WithdrawalPendingLoginException(email)
        }
    }

    fun withdrawnEmail(): Email = Email.withdrawn(id)

    fun maskedPhoneNumber(): String = phoneNumber.masked()

    fun anonymize(): Member {
        return Member(
            id = id,
            email = withdrawnEmail(),
            password = WithdrawnSentinel.PASSWORD,
            nickname = WithdrawnSentinel.nickname(id),
            gender = gender,
            phoneNumber = PhoneNumber(WithdrawnSentinel.PHONE_NUMBER),
            name = WithdrawnSentinel.NAME,
            region = WithdrawnSentinel.REGION,
            birthDate = WithdrawnSentinel.BIRTH_DATE,
            highSchool = null,
            university = null,
            withdrawalRequestedAt = withdrawalRequestedAt,
        )
    }

    private fun isWithdrawalRequested(): Boolean = withdrawalRequestedAt != null
}

