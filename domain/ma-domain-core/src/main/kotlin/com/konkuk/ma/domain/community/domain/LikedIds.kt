package com.konkuk.ma.domain.community.domain

class LikedIds(val data: Set<Long>) {

    fun contains(id: Long): Boolean = id in data
}
