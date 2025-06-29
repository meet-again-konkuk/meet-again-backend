package com.konkuk.ma.member.domain.port

import com.konkuk.ma.member.domain.NewMember

interface MemberCommandRepository {
    fun save(newMember: NewMember): Long
} 
