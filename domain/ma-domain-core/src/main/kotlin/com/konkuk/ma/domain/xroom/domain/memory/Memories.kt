package com.konkuk.ma.domain.xroom.domain.memory

class Memories(
    val data: List<Memory>,
) {
    fun sortedByEventDate(): List<Memory> = data.sortedWith(compareBy({ it.sortKey() }, { it.id }))

    fun extractIds(): Set<Long> = data.map { it.id }.toSet()
}
