package com.konkuk.ma.domain.community.domain.block

import com.konkuk.ma.domain.member.domain.Members

class Blocks(val data: List<Block>) {

    fun extractBlockedIds(): Set<Long> {
        return data.map { it.blockedId }.toSet()
    }

    fun combineWithViews(members: Members): List<BlockView> {
        return data.map { it.combineWithView(members) }
    }
}
