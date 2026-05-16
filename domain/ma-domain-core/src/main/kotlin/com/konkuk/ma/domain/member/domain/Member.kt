package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.member.domain.policy.WithdrawalGraceWindow
import com.konkuk.ma.domain.member.domain.policy.WithdrawalPolicy
import com.konkuk.ma.domain.member.domain.policy.WithdrawnSentinel
import com.konkuk.ma.domain.member.exception.AlreadyWithdrawalRequestedException
import com.konkuk.ma.domain.member.exception.NotWithdrawalRequestedException
import java.time.LocalDate
import java.time.LocalDateTime

class Member(
    val id: Long = 0L,
    val email: Email,
    val password: String,
    val nickname: String,
    val gender: Gender,
    val phoneNumber: PhoneNumber,
    val name: String,
    val region: Region,
    val birthDate: LocalDate,
    val highSchool: String?,
    val university: String?,
    withdrawalRequestedAt: LocalDateTime? = null,
) {
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

    fun isActive(): Boolean = withdrawalRequestedAt == null

    fun isWithdrawalRequested(): Boolean = withdrawalRequestedAt != null

    fun isWithdrawalPending(now: LocalDateTime): Boolean {
        val requestedAt = withdrawalRequestedAt ?: return false
        return !WithdrawalPolicy.isExpired(requestedAt, now)
    }

    fun isWithdrawalExpired(now: LocalDateTime): Boolean {
        val requestedAt = withdrawalRequestedAt ?: return false
        return WithdrawalPolicy.isExpired(requestedAt, now)
    }

    fun withdrawalGraceWindowOrNull(now: LocalDateTime): WithdrawalGraceWindow? {
        val requestedAt = withdrawalRequestedAt ?: return null
        val expiresAt = WithdrawalPolicy.expiresAt(requestedAt)
        if (!now.isBefore(expiresAt)) return null
        return WithdrawalGraceWindow(requestedAt, expiresAt)
    }

    fun anonymize(): Member {
        return Member(
            id = id,
            email = Email.withdrawn(id),
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
}

