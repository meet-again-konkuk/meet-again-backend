package com.konkuk.ma.domain.member.fixture

import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.NewMember
import com.konkuk.ma.domain.member.domain.PhoneNumber
import com.konkuk.ma.domain.member.domain.Region
import java.time.LocalDate

object NewMemberFixture {
    fun create(
        email: String = "test@example.com",
        password: String = "password123",
        nickname: String = "testuser",
        gender: Gender = Gender.MALE,
        phoneNumber: PhoneNumber = PhoneNumber("01012345678"),
        name: String = "김테스트",
        birthDate: LocalDate = LocalDate.of(1990, 1, 1),
        region: Region = Region.SEOUL,
        highSchool: String? = null,
        university: String? = null
    ): NewMember {
        return NewMember(
            email = email,
            password = password,
            nickname = nickname,
            gender = gender,
            phoneNumber = phoneNumber,
            name = name,
            birthDate = birthDate,
            region = region,
            highSchool = highSchool,
            university = university
        )
    }
}
