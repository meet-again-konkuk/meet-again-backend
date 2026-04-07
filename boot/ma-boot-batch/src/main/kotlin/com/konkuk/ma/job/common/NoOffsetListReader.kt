package com.konkuk.ma.job.common

import org.springframework.batch.item.ItemReader

abstract class NoOffsetListReader<T, C>(
    private val readSize: Int = 1000,
    private val readFunction: (cursorId: C?, limit: Int) -> List<T>,
    private val cursorIdExtractor: (T) -> C,
) : ItemReader<List<T>> {

    private var lastCursorId: C? = null
    private var finished: Boolean = false

    override fun read(): List<T>? {
        if (finished) return null

        val items = readFunction(lastCursorId, readSize)

        if (items.isEmpty()) {
            finished = true
            return null
        }

        lastCursorId = cursorIdExtractor(items.last())
        return items
    }
}
