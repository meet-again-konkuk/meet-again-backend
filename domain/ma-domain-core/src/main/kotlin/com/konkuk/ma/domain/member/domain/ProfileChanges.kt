package com.konkuk.ma.domain.member.domain

import com.konkuk.ma.domain.common.domain.Changed

class ProfileChanges(
    val nickname: String?,
    val region: Region?,
    val highSchool: Changed<String>?,
    val university: Changed<String>?,
)
