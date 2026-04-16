package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.xroom.domain.NewXroom

interface XroomCommandRepository {
    fun save(newXroom: NewXroom): Long
}
