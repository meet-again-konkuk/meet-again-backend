package com.konkuk.ma.domain.community.domain.block

class BlockedMemberIds(val data: Set<Long>) {

    fun contains(authorId: Long): Boolean = authorId in data
}
