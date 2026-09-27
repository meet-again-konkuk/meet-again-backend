package com.konkuk.ma.domain.matching.fixture

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.PhoneNumber
import com.konkuk.ma.domain.member.domain.Region
import java.time.LocalDate

object MemberFixture {
    fun create(
        id: Long = 0L,
        email: String = "target@example.com",
        password: String = "password",
        nickname: String = "nickname",
        gender: Gender = Gender.MALE,
        phoneNumber: String = "01012345678",
        name: String = "홍길동",
        region: Region = Region.SEOUL,
        birthDate: LocalDate = LocalDate.of(1999, 12, 31),
        highSchool: String? = null,
        university: String? = null
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
