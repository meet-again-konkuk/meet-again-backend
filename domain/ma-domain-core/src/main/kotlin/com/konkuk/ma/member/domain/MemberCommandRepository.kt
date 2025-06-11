package com.konkuk.ma.member.domain

interface MemberCommandRepository {
    fun save(member: Member): Long
} 