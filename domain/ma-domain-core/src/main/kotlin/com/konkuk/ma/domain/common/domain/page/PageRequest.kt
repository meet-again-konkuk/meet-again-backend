package com.konkuk.ma.domain.common.domain.page

class PageRequest(
    val page: Int,
    val size: Int,
) {
    val offset: Long get() = page.toLong() * size
}
