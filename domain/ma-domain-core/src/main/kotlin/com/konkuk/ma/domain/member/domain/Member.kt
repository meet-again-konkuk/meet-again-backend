package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.common.domain.Day
import com.konkuk.ma.domain.common.domain.Month
import com.konkuk.ma.domain.common.domain.Year
import com.konkuk.ma.domain.member.domain.port.PasswordEncryptor
import java.time.LocalDate

class Member(
    val email: String,
    val password: String,
    val nickname: String,
    val gender: Gender,
    val phoneNumber: PhoneNumber,
    val name: String,
    val region: Region,
    val birthDate: LocalDate,
    val highSchool: String?,
    val university: String?
) {
    companion object {
        fun create(
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
                email = email,
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

    fun matches(inputPassword: String, passwordEncryptor: PasswordEncryptor): Boolean {
        if (!passwordEncryptor.matches(inputPassword, password)) {
            throw IllegalArgumentException("비밀번호가 올바르지 않습니다.")
        }
        return true
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
} 
