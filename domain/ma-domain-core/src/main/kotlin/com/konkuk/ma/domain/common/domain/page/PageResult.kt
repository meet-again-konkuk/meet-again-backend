package com.konkuk.ma.domain.common.domain.page

class PageResult<T>(
    val data: T,
    val totalCount: Long,
    val currentPage: Int,
    val pageSize: Int,
) {
    fun hasNext(): Boolean = (currentPage + 1) * pageSize < totalCount
}
