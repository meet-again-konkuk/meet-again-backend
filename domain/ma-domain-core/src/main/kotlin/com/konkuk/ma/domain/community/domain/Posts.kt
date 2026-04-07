package com.konkuk.ma.domain.community.domain

class Posts(
    val data: List<Post>,
    val totalCount: Long,
    val currentPage: Int,
) {
    fun hasNext(): Boolean {
        return (currentPage + 1) * PAGE_SIZE < totalCount
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
