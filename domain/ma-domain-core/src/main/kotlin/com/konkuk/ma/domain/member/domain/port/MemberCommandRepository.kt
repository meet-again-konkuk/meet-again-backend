package com.konkuk.ma.domain.member.domain.port

import com.konkuk.ma.domain.member.domain.NewMember

interface MemberCommandRepository {
    fun save(newMember: NewMember): Long
} 
